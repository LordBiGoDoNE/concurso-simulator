package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.ApplyQuestionCandidatesUseCase;
import br.com.concursosimulator.questionbank.application.ApplyQuestionCandidatesUseCase.Candidate;
import br.com.concursosimulator.questionbank.application.DecideQuestionUseCase;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.*;
import static br.com.concursosimulator.questionbank.QuestionFixtures.*;
import static br.com.concursosimulator.questionbank.QuestionEditorialTests.decision;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class QuestionImportTests {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18.6");
    QuestionDatabase db; ApplyQuestionCandidatesUseCase apply;
    @BeforeEach void open(){db=new QuestionDatabase(POSTGRES);apply=new ApplyQuestionCandidatesUseCase(db.store);}
    @AfterEach void close(){db.close();}
    Candidate candidate(String key,String text,UUID base){return new Candidate(new QuestionOrigin("fixture",key),content(text),base);}
    Question load(UUID id){return db.store.transaction(s->s.loadForUpdate(id).orElseThrow());}
    @Test void repeatedAndOldWithdrawnContentPreserveIdsVersionsAndEditorialHistory() {
        var original=candidate("same","Fixture",null);var first=apply.execute(List.of(original)).getFirst();
        assertThat(load(first.questionId()).eligible()).isEmpty();
        var editorial=new DecideQuestionUseCase(db.store);var q=load(first.questionId());
        editorial.execute(decision(q,first.revisionId(),SUBMIT),null);editorial.execute(decision(q,first.revisionId(),APPROVE),null);
        editorial.execute(decision(q,first.revisionId(),WITHDRAW),first.revisionId());
        var next=apply.execute(List.of(candidate("same","Nova versão",first.revisionId()))).getFirst();
        var repeated=apply.execute(List.of(original)).getFirst();
        assertThat(repeated.questionId()).isEqualTo(first.questionId());assertThat(repeated.revisionId()).isEqualTo(first.revisionId());assertThat(repeated.reused()).isTrue();
        q=load(first.questionId());assertThat(q.revisions()).hasSize(2);assertThat(q.latest().id()).isEqualTo(next.revisionId());
        assertThat(q.eligible()).isEmpty();assertThat(q.decisions()).hasSize(3);assertThat(q.status(next.revisionId())).isEqualTo(Question.Status.DRAFT);
    }
    @Test void staleChangedContentRollsBackTheWholePackageAndNeverDeletesAbsentOrigins() {
        var old=apply.execute(List.of(candidate("z","Original",null))).getFirst();
        assertThatThrownBy(()->apply.execute(List.of(candidate("a","Novo",null),candidate("z","Alterado",UUID.randomUUID())))).isInstanceOf(StaleQuestionException.class);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_question",Integer.class)).isEqualTo(1);
        assertThat(load(old.questionId()).revisions()).hasSize(1);
        apply.execute(List.of(candidate("other","Outro",null)));
        assertThat(load(old.questionId()).latest().id()).isEqualTo(old.revisionId());
        assertThatThrownBy(()->apply.execute(List.of(candidate("a","X",null),candidate("a","Y",null)))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void knownOriginConflictIsRecoveredOnlyAfterRollbackInANewUnit() {
        var completed=new AtomicInteger(); var attempts=new AtomicInteger();
        var fake=new QuestionStore(){
            public <T>T transaction(Function<Session,T> operation){
                if(attempts.incrementAndGet()==1){completed.incrementAndGet();throw new OriginAlreadyExistsException(new RuntimeException("fixture"));}
                assertThat(completed.get()).isEqualTo(1);return db.store.transaction(operation);
            }
        };
        assertThat(new ApplyQuestionCandidatesUseCase(fake).execute(List.of(candidate("retry","Fixture",null)))).hasSize(1);
        assertThat(attempts.get()).isEqualTo(2);
    }
    @Test void unknownPrimaryKeyForeignKeyAndUnavailableFailuresAreNotOriginConflictsOrSuccess() {
        var first=apply.execute(List.of(candidate("first","Fixture",null))).getFirst();
        var duplicateId=Question.draft(first.questionId(),new QuestionOrigin("fixture","different"),UUID.randomUUID(),content("Outra"));
        assertThatThrownBy(()->db.store.transaction(s->{s.insert(duplicateId);return null;})).isNotInstanceOf(OriginAlreadyExistsException.class);
        assertThatThrownBy(()->db.store.transaction(s->{db.jdbc.update("INSERT INTO qb_revision_role VALUES (?, 'assistente-administrativo')",UUID.randomUUID());return null;})).isNotInstanceOf(OriginAlreadyExistsException.class);
        var attempts=new AtomicInteger();var failed=new QuestionStore(){public <T>T transaction(Function<Session,T> operation){attempts.incrementAndGet();throw new IllegalStateException("indisponível");}};
        assertThatThrownBy(()->new ApplyQuestionCandidatesUseCase(failed).execute(List.of(candidate("new","X",null)))).isInstanceOf(IllegalStateException.class);
        assertThat(attempts.get()).isEqualTo(1);assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_question",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentIdenticalImportsForceTheMissingOriginRaceAndKeepOnlyOneQuestionRevision() throws Exception {
        var missing=new CountDownLatch(6);var attempts=new AtomicInteger();var collisions=new AtomicInteger();
        var racing=new QuestionStore(){
            public <T>T transaction(Function<Session,T> operation){
                attempts.incrementAndGet();
                try{return db.store.transaction(s->operation.apply(new Session(){
                    public Optional<Question> loadForUpdate(UUID id){return s.loadForUpdate(id);}
                    public Optional<Question> findOriginForUpdate(QuestionOrigin origin){
                        var found=s.findOriginForUpdate(origin);
                        if(found.isEmpty()) {missing.countDown();try{assertThat(missing.await(5,TimeUnit.SECONDS)).isTrue();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
                        return found;
                    }
                    public void insert(Question q){s.insert(q);}public void save(Question q){s.save(q);}
                }));}catch(OriginAlreadyExistsException expected){collisions.incrementAndGet();throw expected;}
            }
        };
        try(var pool=Executors.newFixedThreadPool(6)){
            var futures=new ArrayList<Future<ApplyQuestionCandidatesUseCase.Result>>();
            for(int i=0;i<6;i++)futures.add(pool.submit(()->new ApplyQuestionCandidatesUseCase(racing).execute(List.of(candidate("parallel","πr²",null))).getFirst()));
            var results=new ArrayList<ApplyQuestionCandidatesUseCase.Result>();for(var f:futures)results.add(f.get(15,TimeUnit.SECONDS));
            assertThat(results.stream().map(ApplyQuestionCandidatesUseCase.Result::questionId).distinct()).hasSize(1);
            assertThat(results.stream().map(ApplyQuestionCandidatesUseCase.Result::revisionId).distinct()).hasSize(1);
        }
        assertThat(collisions.get()).isEqualTo(5);assertThat(attempts.get()).isEqualTo(11);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_question",Integer.class)).isEqualTo(1);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_revision",Integer.class)).isEqualTo(1);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_decision",Integer.class)).isZero();
    }
    @Test void concurrentMultiOriginPackagesInOppositeInputOrderUseTheSameLockOrderAndCommitAtomically() throws Exception {
        var bothMissing=new CountDownLatch(2);var attempts=new AtomicInteger();
        var racing=new QuestionStore(){
            public <T>T transaction(Function<Session,T> operation){
                attempts.incrementAndGet();return db.store.transaction(s->operation.apply(new Session(){
                    public Optional<Question> loadForUpdate(UUID id){return s.loadForUpdate(id);}
                    public Optional<Question> findOriginForUpdate(QuestionOrigin origin){
                        var found=s.findOriginForUpdate(origin);
                        if(found.isEmpty() && origin.key().equals("a")) {
                            bothMissing.countDown();
                            try{assertThat(bothMissing.await(5,TimeUnit.SECONDS)).isTrue();}
                            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                        }
                        return found;
                    }
                    public void insert(Question q){s.insert(q);}public void save(Question q){s.save(q);}
                }));
            }
        };
        var a=candidate("a","Primeira fixture",null);var z=candidate("z","Segunda fixture",null);
        try(var pool=Executors.newFixedThreadPool(2)){
            var importer=new ApplyQuestionCandidatesUseCase(racing);
            var first=pool.submit(()->importer.execute(List.of(a,z)));
            var second=pool.submit(()->importer.execute(List.of(z,a)));
            var one=first.get(15,TimeUnit.SECONDS);var two=second.get(15,TimeUnit.SECONDS);
            assertThat(one.stream().map(ApplyQuestionCandidatesUseCase.Result::questionId).toList())
                    .isEqualTo(two.stream().map(ApplyQuestionCandidatesUseCase.Result::questionId).toList());
            assertThat(one.stream().map(ApplyQuestionCandidatesUseCase.Result::revisionId).toList())
                    .isEqualTo(two.stream().map(ApplyQuestionCandidatesUseCase.Result::revisionId).toList());
            assertThat(one.stream().map(ApplyQuestionCandidatesUseCase.Result::reused).distinct().toList()).hasSize(1);
            assertThat(two.stream().map(ApplyQuestionCandidatesUseCase.Result::reused).distinct().toList()).hasSize(1);
            assertThat(one.getFirst().reused()).isNotEqualTo(two.getFirst().reused());
        }
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_question",Integer.class)).isEqualTo(2);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_revision",Integer.class)).isEqualTo(2);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_decision",Integer.class)).isZero();
    }
}

package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaEligibleRevisionPool;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class ParameterizedPersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    static final Instant NOW=Instant.parse("2026-10-15T12:00:00Z");
    static final AttemptOwner VISITOR=new AttemptOwner(AttemptOwner.Kind.VISITOR,UUID.randomUUID());
    static Composition composition(boolean enabled) { return new Composition(TargetRole.SCHOOL_SECRETARY,
            Map.of(Discipline.MATHEMATICS_LOGIC,new Composition.Portion(1,Set.of())),enabled); }
    static JpaAttemptStore store(ParameterDatabase db) { return new JpaAttemptStore(db.entities,db.manager,new JpaEligibleRevisionPool(db.entities),db.eligible); }
    static AttemptUseCases cases(AttemptStore s) {return new AttemptUseCases(s,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,()->new Random(7));}
    @Test void accountHistoryAndResultSurviveNewRepositoryAndBaseWithdrawal() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);db.approve(d);
            var owner=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
            db.jdbc.update("insert into app_user(id) values (?)",owner.id());
            var created=cases(store(db)).create(owner,UUID.randomUUID(),composition(true)).attempt();
            var item=store(db).transaction(tx->tx.find(created.id()).orElseThrow()).items().getFirst();
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.WITHDRAW);
            var onlyFrozen=new JpaAttemptStore(db.entities,db.manager,()->{throw new AssertionError();},()->{throw new AssertionError();});
            var nextSession=cases(onlyFrozen);
            nextSession.answer(owner,created.id(),UUID.randomUUID(),0,item.id(),item.question().content().correctAlternativeId());
            var result=nextSession.finish(owner,created.id(),UUID.randomUUID(),1).result();
            assertThat(result.correct()).isEqualTo(1);
            assertThat(cases(onlyFrozen).history(owner,new AttemptStore.HistoryQuery(0,10,true))).singleElement()
                    .satisfies(e->assertThat(e.percentage()).isEqualByComparingTo("100.00"));
            assertThat(cases(onlyFrozen).result(owner,created.id())).isEqualTo(result);
            assertThatThrownBy(()->cases(onlyFrozen).result(new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID()),created.id()))
                    .isInstanceOf(Attempt.Unavailable.class);
        }
    }
    @Test void frozenInstanceSurvivesWithdrawalAndCompatibleRollbackWithoutCatalogAccess() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.SIMPLE_INTEREST);var v=db.approve(d);var s=store(db);var op=UUID.randomUUID();
            var created=cases(s).create(VISITOR,op,composition(true)).attempt();
            var snapshot=s.transaction(tx->tx.find(created.id()).orElseThrow());var item=snapshot.items().getFirst();
            assertThat(db.jdbc.queryForObject("select count(*) from pq_instance",Integer.class)).isEqualTo(1);
            var uc=new TemplateUseCases(db.store);uc.decide(ParameterFixtures.decision(v,TemplateDecision.Action.WITHDRAW),1,2);
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.WITHDRAW);
            var unavailable=new JpaAttemptStore(db.entities,db.manager,()->{throw new AssertionError("Sem banco atual");},
                    ()->{throw new AssertionError("Sem gerador/template atual");});
            var rollback=new AttemptUseCases(unavailable,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
            assertThat(rollback.read(VISITOR,created.id())).isEqualTo(created);
            assertThat(rollback.create(VISITOR,op,composition(true)).attempt()).isEqualTo(created);
            assertThatThrownBy(()->rollback.create(VISITOR,UUID.randomUUID(),composition(true))).isInstanceOf(IllegalStateException.class);
            rollback.answer(VISITOR,created.id(),UUID.randomUUID(),0,item.id(),item.question().content().correctAlternativeId());
            assertThat(rollback.finish(VISITOR,created.id(),UUID.randomUUID(),1).result().correct()).isEqualTo(1);
            assertThat(rollback.result(VISITOR,created.id()).items().getFirst().resolution()).isEqualTo(item.question().content().resolution());
            assertThatThrownBy(()->rollback.read(new AttemptOwner(AttemptOwner.Kind.VISITOR,UUID.randomUUID()),created.id()))
                    .isInstanceOf(Attempt.Unavailable.class);
            var expired=new AttemptUseCases(unavailable,Clock.fixed(NOW.plusSeconds(86400),ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
            assertThatThrownBy(()->expired.read(VISITOR,created.id())).isInstanceOf(Attempt.Unavailable.class);
            int removed=s.transaction(tx->tx.deleteExpired(NOW.plusSeconds(86400)));assertThat(removed).isEqualTo(1);
            for(var table:List.of("me_attempt","me_item","me_command","pq_instance"))
                assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from pq_family",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_decision",Integer.class)).isEqualTo(3);
        }
    }
    @Test void sameSnapshotSeesTemplateBeforeWithdrawalAndNextCreationStaysFixed() throws Exception {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);var v=db.approve(d);
            var captured=new CountDownLatch(1);var withdrawn=new CountDownLatch(1);
            var original=new JpaEligibleRevisionPool(db.entities);
            var paused=new JpaAttemptStore(db.entities,db.manager,()->{
                var pool=original.capture();captured.countDown();await(withdrawn);return pool;
            },db.eligible);
            try(var executor=Executors.newSingleThreadExecutor()) {
                var future=executor.submit(()->cases(paused).create(VISITOR,UUID.randomUUID(),composition(true)).attempt());
                await(captured);new TemplateUseCases(db.store).decide(ParameterFixtures.decision(v,TemplateDecision.Action.WITHDRAW),1,2);
                withdrawn.countDown();var first=future.get(20,TimeUnit.SECONDS);
                var s=store(db);assertThat(s.transaction(tx->tx.find(first.id()).orElseThrow()).items().getFirst().parameters()).isNotEmpty();
                var next=cases(s).create(VISITOR,UUID.randomUUID(),composition(true)).attempt();
                assertThat(s.transaction(tx->tx.find(next.id()).orElseThrow()).items().getFirst().parameters()).isEmpty();
            }
        }
    }
    @Test void concurrentLostAcknowledgementReturnsWinnerWithoutOrphanInstance() throws Exception {
        try(var db=new ParameterDatabase(PG)) {
            db.approve(db.seed(Family.DIRECT_PROPORTION));var barrier=new CyclicBarrier(2);
            var s=new JpaAttemptStore(db.entities,db.manager,new JpaEligibleRevisionPool(db.entities),()->{
                var result=db.eligible.capture();try{barrier.await(10,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException(e);}return result;
            });
            var op=UUID.randomUUID();
            try(var executor=Executors.newFixedThreadPool(2)) {
                var a=executor.submit(()->cases(s).create(VISITOR,op,composition(true)));
                var b=executor.submit(()->cases(s).create(VISITOR,op,composition(true)));
                var first=a.get(25,TimeUnit.SECONDS);var second=b.get(25,TimeUnit.SECONDS);
                assertThat(first.attempt()).isEqualTo(second.attempt());assertThat(first.replayed()).isNotEqualTo(second.replayed());
                assertThat(cases(store(db)).create(VISITOR,op,composition(true)).attempt()).isEqualTo(first.attempt());
            }
            for(var table:List.of("me_attempt","me_item","me_command","pq_instance"))
                assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isEqualTo(1);
        }
    }
    @Test void immutableInstanceAndDeferredLinkRejectPartialOrDivergentCaptures() {
        try(var db=new ParameterDatabase(PG)) {
            db.approve(db.seed(Family.PERCENTAGE));var s=store(db);
            var created=cases(s).create(VISITOR,UUID.randomUUID(),composition(true)).attempt();
            assertThatThrownBy(()->db.jdbc.update("update pq_instance set template_version=99")).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(()->db.jdbc.update("delete from pq_instance")).isInstanceOf(DataIntegrityViolationException.class);
            var old=s.transaction(tx->tx.find(created.id()).orElseThrow());var item=old.items().getFirst();
            var nextItem=new Attempt.Item(UUID.randomUUID(),item.question(),item.optionOrder(),item.parameters());
            var next=Attempt.start(UUID.randomUUID(),VISITOR,composition(true),NOW,List.of(nextItem)).snapshot();
            var json=JsonMapper.builder().build();
            assertThatThrownBy(()->new TransactionTemplate(db.manager).execute(status->{
                db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,created_at,expires_at,payload) values (?,'VISITOR',?,?,?,?::jsonb)",
                        next.id(),VISITOR.id(),Timestamp.from(NOW),Timestamp.from(NOW.plusSeconds(86400)),json.writeValueAsString(next));
                db.jdbc.update("insert into me_item values (?,?,0,?,?,?::jsonb)",nextItem.id(),next.id(),nextItem.question().questionId(),
                        nextItem.question().revisionId(),json.writeValueAsString(nextItem));return null;
            })).isInstanceOf(RuntimeException.class);
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(1);
            var c=item.question().source().capture();
            var q=item.question();var content=q.content();
            var unknown=new TemplateDefinition(UUID.randomUUID(),c.templateVersion(),c.parameters().family(),c.generatorVersion(),
                    q.questionId(),q.revisionId(),c.baseFingerprint(),content.classification(),content.references(),content.provenance());
            var bad=new QuestionSource.Capture(unknown.id(),c.templateVersion(),unknown.digest(),c.domainDigest(),VariantManifest.create(unknown).digest(),
                    CapturedOutputDigest.fromContent(unknown.digest(),c.parameters(),content),c.generatorVersion(),c.baseRevisionId(),c.baseFingerprint(),c.renderedFingerprint(),c.parameters());
            var badQuestion=new AttemptQuestion(q.questionId(),q.revisionId(),q.fingerprint(),q.content(),
                    new QuestionSource(QuestionSource.Kind.PARAMETERIZED,bad));
            var invalid=Attempt.start(UUID.randomUUID(),VISITOR,composition(true),NOW,List.of(
                    new Attempt.Item(UUID.randomUUID(),badQuestion,item.optionOrder(),item.parameters()))).snapshot();
            assertThatThrownBy(()->s.transaction(tx->{tx.insert(invalid);return null;})).isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(AttemptStore.ConcurrentWrite.class);
            var changedOutput=new QuestionSource.Capture(c.templateId(),c.templateVersion(),c.definitionDigest(),c.domainDigest(),c.manifestDigest(),
                    "0".repeat(64),c.generatorVersion(),c.baseRevisionId(),c.baseFingerprint(),c.renderedFingerprint(),c.parameters());
            assertThatThrownBy(()->new AttemptQuestion(q.questionId(),q.revisionId(),q.fingerprint(),q.content(),
                    new QuestionSource(QuestionSource.Kind.PARAMETERIZED,changedOutput))).isInstanceOf(IllegalArgumentException.class);
            for(var table:List.of("me_attempt","me_item","me_command","pq_instance"))
                assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isEqualTo(1);
        }
    }
    private static void await(CountDownLatch latch) {
        try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("timeout");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
}

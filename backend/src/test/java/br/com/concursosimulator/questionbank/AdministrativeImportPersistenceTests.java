package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaEligibleRevisionPool;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import static org.assertj.core.api.Assertions.*;
import static br.com.concursosimulator.questionbank.AdministrativeImportFixtures.document;

@Testcontainers
class AdministrativeImportPersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    QuestionDatabase db;ApplyAdministrativeQuestionsUseCase apply;
    final Clock clock=Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void open(){db=new QuestionDatabase(PG);apply=new ApplyAdministrativeQuestionsUseCase(db.store,clock);}
    @AfterEach void close(){db.close();}
    QuestionImportPackage pack(QuestionPurpose purpose,String...keys){return new AdministrativePackageReader().read(document(purpose,keys),List.of(keys));}
    ImportConfirmation confirmation(QuestionImportPackage p){return new ImportConfirmation(UUID.randomUUID(),UUID.randomUUID(),"isolated",p.digest(),p.selectionDigest(),clock.instant(),clock.instant().plusSeconds(900));}
    ApplyAdministrativeQuestionsUseCase.Outcome run(QuestionImportPackage p,ImportConfirmation c){return apply.execute(p,c,c.actor(),c.destination());}
    int count(String table){return db.jdbc.queryForObject("select count(*) from "+table,Integer.class);}
    @Test void atomicBothCollectionsReplayAndContentReapplicationPreserveDrafts(){
        for(var purpose:QuestionPurpose.values()){
            var p=pack(purpose,"admin.a");var c=confirmation(p);var first=run(p,c);
            assertThat(first.replayed()).isFalse();assertThat(run(p,c).receipt()).isEqualTo(first.receipt());
            assertThat(run(p,c).replayed()).isTrue();var repeated=run(p,confirmation(p));
            assertThat(repeated.receipt().results().getFirst().reused()).isTrue();
            var result=first.receipt().results().getFirst();var q=db.store.transaction(s->s.loadForUpdate(result.questionId()).orElseThrow());
            assertThat(q.latest().id()).isEqualTo(result.revisionId());assertThat(q.eligible()).isEmpty();assertThat(q.decisions()).isEmpty();
        }
        assertThat(count("qb_question")).isEqualTo(2);assertThat(count("qb_import_blob")).isEqualTo(1);assertThat(count("qb_import_receipt")).isEqualTo(4);
        assertThat(count("qb_decision")).isZero();assertThat(count("qb_import_revision")).isEqualTo(4);
    }
    @Test void failureDuringEvidenceRollsBackQuestionsReceiptAndBlob(){
        db.jdbc.execute("create function fail_fixture() returns trigger language plpgsql as $$ begin raise exception 'fixture failure'; end $$");
        db.jdbc.execute("create trigger fail_fixture before insert on qb_import_file for each row execute function fail_fixture()");
        var p=pack(QuestionPurpose.PRATICA_AULA,"admin.a","admin.b");
        assertThatThrownBy(()->run(p,confirmation(p))).isInstanceOf(RuntimeException.class);
        for(var table:List.of("qb_question","qb_revision","qb_import_receipt","qb_import_blob","qb_import_file","qb_import_revision"))assertThat(count(table)).as(table).isZero();
    }
    @Test void conflictsRejectWholeSelectionAndConfirmationBindsActorDestinationAndPurpose(){
        var p=pack(QuestionPurpose.SIMULADO,"admin.z");run(p,confirmation(p));
        var d=document(QuestionPurpose.SIMULADO,"admin.a","admin.z");var last=d.items().getLast();
        // Same content is idempotent; use a different, valid statement for the conflict.
        var content=last.content();var changed=new br.com.concursosimulator.questionbank.application.port.QuestionImportReader.Content(new TextContent("Changed"),content.alternatives(),content.correctAlternativeId(),content.resolution(),content.classification(),content.references(),content.provenance());
        var domain=new QuestionContent(changed.statement(),changed.alternatives(),changed.correctAlternativeId(),changed.resolution(),QuestionClassification.fromIds("portugues","P01",changed.classification().roleIds()),changed.references(),changed.provenance());
        var item=new AdministrativePackageReader.Item(last.origin(),changed,domain.fingerprint(),UUID.randomUUID());
        var stale=new AdministrativePackageReader().read(new AdministrativePackageReader.Document(d.format(),d.purpose(),List.of(d.items().getFirst(),item),d.files()),List.of("admin.a","admin.z"));
        assertThatThrownBy(()->run(stale,confirmation(stale))).isInstanceOf(StaleQuestionException.class);
        assertThat(count("qb_question")).isEqualTo(1);assertThat(count("qb_import_receipt")).isEqualTo(1);
        var c=confirmation(p);
        assertThatThrownBy(()->apply.execute(p,c,UUID.randomUUID(),c.destination())).isInstanceOf(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure.class);
        assertThatThrownBy(()->apply.execute(p,c,c.actor(),"other")).isInstanceOf(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure.class);
        var expired=new ApplyAdministrativeQuestionsUseCase(db.store,Clock.offset(clock,Duration.ofMinutes(16)));
        assertThatThrownBy(()->expired.execute(p,c,c.actor(),c.destination())).isInstanceOf(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure.class);
    }
    @Test void concurrentSameNonceAndDifferentNonceKeepSingleIdentity()throws Exception {
        var p=pack(QuestionPurpose.PRATICA_AULA,"admin.parallel");var c=confirmation(p);
        try(var workers=Executors.newFixedThreadPool(2)){
            var first=workers.submit(()->run(p,c));var second=workers.submit(()->run(p,c));
            assertThat(first.get(20,TimeUnit.SECONDS).receipt()).isEqualTo(second.get(20,TimeUnit.SECONDS).receipt());
            var a=workers.submit(()->run(p,confirmation(p)));var b=workers.submit(()->run(p,confirmation(p)));
            assertThat(a.get(20,TimeUnit.SECONDS).receipt().results().getFirst().questionId()).isEqualTo(b.get(20,TimeUnit.SECONDS).receipt().results().getFirst().questionId());
        }
        assertThat(count("qb_question")).isEqualTo(1);assertThat(count("qb_revision")).isEqualTo(1);assertThat(count("qb_import_receipt")).isEqualTo(3);
    }
    @Test void growingPracticePoolStaysSeparateAndHistoricalOriginIsPreserved(){
        var historical=new ApplyQuestionCandidatesUseCase(db.store).execute(List.of(new ApplyQuestionCandidatesUseCase.Candidate(new QuestionOrigin("lesson-practice-v1","P01-P01"),QuestionFixtures.content("Historical"),null))).getFirst();
        var p=pack(QuestionPurpose.PRATICA_AULA,"admin.1","admin.2","admin.3","admin.4","admin.5","admin.6");
        var results=run(p,confirmation(p)).receipt().results();
        var pool=new JpaEligibleRevisionPool(SharedEntityManagerCreator.createSharedEntityManager(db.factory));
        var initial=db.store.transaction(s->pool.capture(QuestionPurpose.PRATICA_AULA));assertThat(initial).isEmpty();
        for(var result:results) {
            for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE)){
                var q=db.store.transaction(s->s.loadForUpdate(result.questionId()).orElseThrow());
                new DecideQuestionUseCase(db.store).execute(new EditorialDecision(action,q.id(),q.latest().id(),q.latest().fingerprint(),"Synthetic fixture",clock.instant(),Set.of(EditorialDecision.Check.values()),"Fixture only"),null,q.editorialSequence());
            }
        }
        var practice=db.store.transaction(s->pool.capture(QuestionPurpose.PRATICA_AULA));assertThat(practice).hasSize(6).allSatisfy(e->assertThat(e.content().classification().topic()).isEqualTo(Topic.P01));
        var simulated=db.store.transaction(s->pool.capture(QuestionPurpose.SIMULADO));assertThat(simulated).isEmpty();
        assertThat(db.store.transaction(s->s.loadForUpdate(historical.questionId()).orElseThrow()).origin().key()).isEqualTo("P01-P01");
    }
}

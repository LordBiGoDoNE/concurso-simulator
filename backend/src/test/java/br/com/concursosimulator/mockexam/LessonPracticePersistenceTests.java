package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class LessonPracticePersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    void seedPractice(AttemptDatabase db,int count) {
        for(int i=0;i<count;i++) {
            var c=br.com.concursosimulator.questionbank.QuestionFixtures.content("Prática própria " + i);
            var q=Question.draft(UUID.randomUUID(),new QuestionOrigin(QuestionPurpose.PRACTICE_NAMESPACE,"P01-P"+i),UUID.randomUUID(),c);
            db.questions.transaction(s -> {s.insert(q);return null;});
            db.decide(q.id(),EditorialDecision.Action.SUBMIT);db.decide(q.id(),EditorialDecision.Action.APPROVE);
        }
    }
    @Test void realDatabaseSeparatesModesBeforeLimitsAndPreservesLegacyCaptureJson() {
        try(var db=new AttemptDatabase(PG)) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());db.seed("Legado reservado",true);
            var store=new JpaAttemptStore(db.entities,db.manager,db.pool);
            var old=new AttemptUseCases(store,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,()->new Random(17));
            var mock=old.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var payload=db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,mock.id());
            db.migrate("11");seedPractice(db,6);
            assertThat(db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,mock.id())).isEqualTo(payload);
            var practice=LessonPracticeUseCasesTests.practice(store,NOW.plusSeconds(1));
            var started=practice.createPractice(ACCOUNT,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt();
            assertThat(started.items()).hasSize(5);assertThat(practice.read(ACCOUNT,started.id())).isEqualTo(started);
            assertThat(old.history(ACCOUNT,new AttemptStore.HistoryQuery(0,1,null))).singleElement().satisfies(e->assertThat(e.id()).isEqualTo(mock.id()));
            assertThat(practice.history(ACCOUNT,new AttemptStore.HistoryQuery(0,1,null,QuestionPurpose.PRATICA_AULA))).singleElement().satisfies(e->assertThat(e.id()).isEqualTo(started.id()));
            assertThatThrownBy(()->old.read(ACCOUNT,started.id())).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(()->practice.read(ACCOUNT,mock.id())).isInstanceOf(Attempt.Unavailable.class);
            var i=started.items().getFirst();practice.answer(ACCOUNT,started.id(),UUID.randomUUID(),0,i.id(),i.alternatives().getFirst().id());
            practice.finish(ACCOUNT,started.id(),UUID.randomUUID(),1);
            old.finish(ACCOUNT,mock.id(),UUID.randomUUID(),0);
            var reader=new JpaCompletedAttempts(db.entities,db.manager);
            assertThat(reader.recent(ACCOUNT,TargetRole.ADMINISTRATIVE_ASSISTANT,1,NOW.plusSeconds(2)).attempts()).hasSize(1);
            assertThatThrownBy(()->reader.one(ACCOUNT,started.id(),NOW.plusSeconds(2))).isInstanceOf(Attempt.Unavailable.class);
            assertThat(db.jdbc.queryForList("select distinct purpose from qb_question order by 1",String.class)).containsExactly("PRATICA_AULA","SIMULADO");
        }
    }
    @Test void databaseRejectsChangingThePoolOrAStoredAttemptContext() {
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("11");seedPractice(db,1);
            var q=db.jdbc.queryForObject("select id from qb_question where purpose='PRATICA_AULA'",UUID.class);
            assertThatThrownBy(()->db.jdbc.update("update qb_question set origin_namespace='legacy-v3' where id=?",q)).isInstanceOf(org.springframework.dao.DataAccessException.class);
            var practice=LessonPracticeUseCasesTests.practice(new JpaAttemptStore(db.entities,db.manager,db.pool),NOW);
            var v=practice.createPractice(VISITOR,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt();
            assertThatThrownBy(()->db.jdbc.update("update me_attempt set payload=jsonb_set(jsonb_set(payload,'{purpose}',to_jsonb('SIMULADO'::text)),'{version}',to_jsonb(version+1)),version=version+1 where id=?",v.id())).isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(practice.read(VISITOR,v.id())).isEqualTo(v);
        }
    }
}

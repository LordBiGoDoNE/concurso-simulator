package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.NOW;
import static org.assertj.core.api.Assertions.*;

/** Fixture serializada por AttemptUseCases/Attempt V5 reais, não pelo codec sob teste. */
@Testcontainers
class V5AttemptCompatibilityTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void upgradePreservesV5HashesRawImmutableJsonAndCommandOutcomesThroughAnswerAndFinish() throws Exception {
        var json=JsonMapper.builder().build();
        var fixture=json.readTree(getClass().getResourceAsStream("/mockexam/v5-creation.json"));
        assertThat(fixture.get("sourceCommit").asString()).isEqualTo("0d0fc4848f55fc93c736aa76dcfbb73a7269eb39");
        var original=fixture.get("snapshot");var oldItem=original.get("items").get(0);var oldQuestion=oldItem.get("question");
        var owner=json.treeToValue(original.get("owner"),AttemptOwner.class);
        var id=UUID.fromString(original.get("id").asString());var operation=UUID.fromString(fixture.get("operationId").asString());
        var itemId=UUID.fromString(oldItem.get("id").asString());
        var content=json.treeToValue(oldQuestion.get("content"),QuestionContent.class);
        var q=Question.draft(UUID.fromString(oldQuestion.get("questionId").asString()),new QuestionOrigin("v5-real-code","frozen"),
                UUID.fromString(oldQuestion.get("revisionId").asString()),content);
        try(var db=new AttemptDatabase(PG,"5")) {
            db.jdbc.update("insert into app_user(id) values (?)",owner.id());
            db.jdbc.update("insert into spring_session values (?,?,1,1,1800,1800000,?)",owner.id().toString(),owner.id().toString(),owner.id().toString());
            db.questions.transaction(s->{s.insert(q);return null;});
            db.decide(q.id(),EditorialDecision.Action.SUBMIT);db.decide(q.id(),EditorialDecision.Action.APPROVE);
            db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) values (?,'ACCOUNT',?,?,?,?::jsonb)",
                    id,owner.id(),owner.id(),Timestamp.from(NOW),original.toString());
            db.jdbc.update("insert into me_item values (?,?,0,?,?,?::jsonb)",itemId,id,q.id(),q.latest().id(),oldItem.toString());
            var hash=fixture.get("command").get("hash").asString();
            assertThat(hash).isEqualTo("2c65452f9a51ac3d5e56c6fa613b9e03ca2aae9d7222d38c515358e1fd43a2be");
            db.jdbc.update("insert into me_command values (?,'ACCOUNT',?,?,?, ?,?::jsonb)",UUID.randomUUID(),owner.id(),operation,id,hash,
                    fixture.get("command").get("outcome").toString());
            var tables=List.of("app_user","spring_session","qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command");
            var before=tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
            var trigger=db.jdbc.queryForObject("select pg_get_functiondef('me_protect_attempt()'::regprocedure)",String.class);
            db.factory.close();db.factory=null;db.migrate("9");db.open();
            assertThat(tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList()).isEqualTo(before);
            assertThat(db.jdbc.queryForObject("select pg_get_functiondef('me_protect_attempt()'::regprocedure)",String.class)).isEqualTo(trigger);
            var s=new JpaAttemptStore(db.entities,db.manager,()->{throw new AssertionError("Replay não seleciona");});
            var uc=new AttemptUseCases(s,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
            var composition=AttemptFixtures.composition(1);
            var absent=uc.create(owner,operation,composition);assertThat(absent.replayed()).isTrue();
            var explicitFalse=new Composition(composition.role(),composition.disciplines(),false);
            assertThat(uc.create(owner,operation,explicitFalse).attempt()).isEqualTo(absent.attempt());
            assertThatThrownBy(()->uc.create(owner,operation,new Composition(composition.role(),composition.disciplines(),true)))
                    .isInstanceOf(Attempt.Conflict.class);
            var answerOp=UUID.randomUUID();var answer=uc.answer(owner,id,answerOp,0,itemId,content.correctAlternativeId());
            assertThat(answer.attempt().version()).isEqualTo(1);
            var finishOp=UUID.randomUUID();assertThat(uc.finish(owner,id,finishOp,1).result().correct()).isEqualTo(1);
            assertThat(uc.answer(owner,id,answerOp,0,itemId,content.correctAlternativeId()).attempt()).isEqualTo(answer.attempt());
            assertThat(uc.finish(owner,id,finishOp,1).replayed()).isTrue();
            assertThat(uc.create(owner,operation,composition).attempt()).isEqualTo(absent.attempt());
            var beforeEvidence=tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
            var reader=new br.com.concursosimulator.mockexam.infrastructure.persistence.JpaCompletedAttempts(db.entities,db.manager);
            var evidence=reader.one(owner,id,NOW);
            assertThat(evidence.items().getFirst().outcome()).isEqualTo(br.com.concursosimulator.mockexam.domain.CompletedAttempt.Outcome.CORRECT);
            var summary=new br.com.concursosimulator.subjectreview.application.ReviewUseCases(reader,Clock.fixed(NOW,ZoneOffset.UTC)).overview(owner,composition.role());
            assertThat(summary.summary().subjects().stream().mapToInt(m->m.correct()).sum()).isEqualTo(1);
            assertThat(summary.window().attemptCount()).isEqualTo(1);
            assertThat(tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList()).isEqualTo(beforeEvidence);
            var finalPayload=json.readTree(db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,id));
            assertThat(finalPayload.has("format")).isFalse();
            for(var field:List.of("composition","items","policy"))assertThat(finalPayload.get(field)).isEqualTo(original.get(field));
            assertThat(json.readTree(db.jdbc.queryForObject("select payload::text from me_item where id=?",String.class,itemId))).isEqualTo(oldItem);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command where outcome ? 'format'",Integer.class)).isZero();
            assertThat(db.jdbc.queryForObject("select payload_hash from me_command where operation_id=?",String.class,operation)).isEqualTo(hash);
            assertThat(uc.history(owner,new AttemptStore.HistoryQuery(0,10,true))).hasSize(1);
            assertThat(s.transaction(tx->tx.find(id).orElseThrow()).format()).isEqualTo(1);
        }
    }
}

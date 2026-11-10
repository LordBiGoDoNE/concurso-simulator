package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static br.com.concursosimulator.mockexam.LessonPracticeUseCasesTests.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class TopicPracticeCompatibilityTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");

    static String legacyHash(Object... values) {
        try {
            var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
            for(var value:values) { var b=String.valueOf(value).getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b); }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException | NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static Attempt.Snapshot legacy(AttemptStore store,AttemptOwner owner,UUID command) {
        return store.transaction(s -> {
            var q=s.eligible(QuestionPurpose.PRATICA_AULA).getFirst();
            var item=new Attempt.Item(UUID.randomUUID(),q,q.content().alternatives().stream().map(QuestionContent.Alternative::id).toList(),Map.of());
            var c=new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,Map.of(Topic.P01.discipline(),new Composition.Portion(1,Set.of(Topic.P01))));
            var snapshot=Attempt.restore(new Attempt.Snapshot(UUID.randomUUID(),owner,c,NOW,null,Attempt.POLICY,List.of(item),Map.of(),0,3,QuestionPurpose.PRATICA_AULA,Topic.P01)).snapshot();
            s.insert(snapshot);s.record(owner,command,new AttemptStore.Command(legacyHash("lesson-practice/v1",c.role().name(),Topic.P01.name()),snapshot));
            return snapshot;
        });
    }
    @Test void migrationPreservesLegacyCapturesCommandsAndAnswersAndNewCodeCanFinishAndReplayThem() throws Exception {
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("11");new LessonPracticePersistenceTests().seedPractice(db,2);
            var store=new JpaAttemptStore(db.entities,db.manager,db.pool);var op=UUID.randomUUID();
            var old=legacy(store,VISITOR,op);var p=practice(store,NOW);var item=old.items().getFirst();
            var answerOp=UUID.randomUUID();var selected=item.optionOrder().getFirst();
            var answered=p.answer(VISITOR,old.id(),answerOp,0,item.id(),selected).attempt();
            var tables=List.of("qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command");
            var before=tables.stream().map(t->db.jdbc.queryForList("select * from "+t+" order by id")).toList();
            db.migrate("14");
            assertThat(tables.stream().map(t->db.jdbc.queryForList("select * from "+t+" order by id")).toList()).isEqualTo(before);
            assertThat(p.read(VISITOR,old.id())).isEqualTo(answered);
            assertThat(p.answer(VISITOR,old.id(),answerOp,0,item.id(),selected).attempt()).isEqualTo(answered);
            assertThatThrownBy(()->p.createPractice(VISITOR,op,Topic.P01)).isInstanceOf(Attempt.Conflict.class);
            var finishOp=UUID.randomUUID();var finished=p.finish(VISITOR,old.id(),finishOp,1);
            assertThat(p.finish(VISITOR,old.id(),finishOp,1).result()).isEqualTo(finished.result());
            assertThat(finished.result().items().getFirst().correctAlternativeId()).isEqualTo(item.question().content().correctAlternativeId());
            assertThat(store.transaction(s->s.find(old.id()).orElseThrow()).composition()).isEqualTo(old.composition());
            assertThat(store.transaction(s->s.find(old.id()).orElseThrow()).format()).isEqualTo(3);
            assertThat(p.read(VISITOR,old.id()).role()).isNull();
            var fresh=p.createPractice(VISITOR,UUID.randomUUID(),Topic.P01).attempt();
            var snapshot=store.transaction(s->s.find(fresh.id()).orElseThrow());
            assertThat(snapshot.format()).isEqualTo(4);assertThat(snapshot.composition()).isNull();
            assertThat(fresh.practiceTopic()).isEqualTo(Topic.P01);assertThat(fresh.role()).isNull();
            restoreCapturedFormats(db,p,old.id(),fresh.id());
        }
    }
    private void restoreCapturedFormats(AttemptDatabase db,AttemptUseCases original,UUID legacyId,UUID freshId) throws Exception {
        var name="lp_restore_"+UUID.randomUUID().toString().replace("-","");var file="/tmp/"+name+".dump";
        assertThat(PG.execInContainer("pg_dump","-U",PG.getUsername(),"-d",PG.getDatabaseName(),"-Fc","-n",db.schema,"-f",file).getExitCode()).isZero();
        assertThat(PG.execInContainer("psql","-U",PG.getUsername(),"-d",PG.getDatabaseName(),"-c","create database "+name).getExitCode()).isZero();
        try {
            assertThat(PG.execInContainer("pg_restore","-U",PG.getUsername(),"-d",name,"--exit-on-error",file).getExitCode()).isZero();
            var url=PG.getJdbcUrl().replace("/"+PG.getDatabaseName()+"?","/"+name+"?")+"&currentSchema="+db.schema;
            try(var restored=new AttemptDatabase(url,PG.getUsername(),PG.getPassword())) {
                for(var table:List.of("qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command"))
                    assertThat(restored.jdbc.queryForList("select row_to_json(t)::text from "+table+" t order by 1",String.class))
                            .isEqualTo(db.jdbc.queryForList("select row_to_json(t)::text from "+table+" t order by 1",String.class));
                var recovered=practice(new JpaAttemptStore(restored.entities,restored.manager,restored.pool),NOW);
                assertThat(recovered.read(VISITOR,legacyId)).isEqualTo(original.read(VISITOR,legacyId));
                assertThat(recovered.result(VISITOR,legacyId)).isEqualTo(original.result(VISITOR,legacyId));
                assertThat(recovered.read(VISITOR,freshId)).isEqualTo(original.read(VISITOR,freshId));
                var item=recovered.read(VISITOR,freshId).items().getFirst();var command=UUID.randomUUID();
                var saved=recovered.answer(VISITOR,freshId,command,0,item.id(),item.alternatives().getFirst().id());
                assertThat(recovered.answer(VISITOR,freshId,command,0,item.id(),item.alternatives().getFirst().id()).attempt()).isEqualTo(saved.attempt());
            }
        } finally {
            assertThat(PG.execInContainer("psql","-U",PG.getUsername(),"-d",PG.getDatabaseName(),"-c","drop database "+name).getExitCode()).isZero();
            assertThat(PG.execInContainer("rm","--",file).getExitCode()).isZero();
        }
    }
    @Test void concurrentCreationUsesOneCapturedSelectionAndRejectsAnIncompatibleTopic() throws Exception {
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("14");new LessonPracticePersistenceTests().seedPractice(db,6);
            var store=new JpaAttemptStore(db.entities,db.manager,db.pool);var p=practice(store,NOW);var command=UUID.randomUUID();
            try(var executor=Executors.newFixedThreadPool(2)) {
                var barrier=new CyclicBarrier(2);
                Callable<AttemptUseCases.Outcome> start=()->{barrier.await(10,TimeUnit.SECONDS);return p.createPractice(VISITOR,command,Topic.P01);};
                var a=executor.submit(start);var b=executor.submit(start);
                var first=a.get(30,TimeUnit.SECONDS);var second=b.get(30,TimeUnit.SECONDS);
                assertThat(first.attempt()).isEqualTo(second.attempt());assertThat(first.replayed()).isNotEqualTo(second.replayed());
            }
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(1);
            assertThatThrownBy(()->p.createPractice(VISITOR,command,Topic.P02)).isInstanceOf(Attempt.Conflict.class);
        }
    }
    @Test void newDatabaseConstraintsRejectHiddenCompositionMissingItemsAndImmutableContextChanges() {
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("14");new LessonPracticePersistenceTests().seedPractice(db,1);
            var p=practice(new JpaAttemptStore(db.entities,db.manager,db.pool),NOW);
            var view=p.createPractice(VISITOR,UUID.randomUUID(),Topic.P01).attempt();
            assertThatThrownBy(()->db.jdbc.update("update me_attempt set payload=jsonb_set(jsonb_set(payload,'{composition}','{}'),'{version}',to_jsonb(version+1)),version=version+1 where id=?",view.id())).isInstanceOf(org.springframework.dao.DataAccessException.class);
            var payload=db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,view.id());
            assertThatThrownBy(()->db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,created_at,expires_at,payload) select ?,owner_kind,owner_id,created_at,expires_at,?::jsonb from me_attempt where id=?",UUID.randomUUID(),payload,view.id())).isInstanceOf(org.springframework.dao.DataAccessException.class);
            var content=br.com.concursosimulator.questionbank.QuestionFixtures.content("Base posterior à captura");
            var extra=Question.draft(UUID.randomUUID(),new QuestionOrigin(QuestionPurpose.PRACTICE_NAMESPACE,"late-item"),UUID.randomUUID(),content);
            db.questions.transaction(s->{s.insert(extra);return null;});db.decide(extra.id(),EditorialDecision.Action.SUBMIT);db.decide(extra.id(),EditorialDecision.Action.APPROVE);
            var q=new AttemptQuestion(extra.id(),extra.latest().id(),content.fingerprint(),content,QuestionSource.fixed(),QuestionPurpose.PRATICA_AULA);
            var item=TopicPracticeDomainTests.item(q);var encoded=tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(item);
            assertThatThrownBy(()->db.jdbc.update("insert into me_item(id,attempt_id,position,question_id,revision_id,payload) values (?,?,1,?,?,?::jsonb)",item.id(),view.id(),q.questionId(),q.revisionId(),encoded)).isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(p.read(VISITOR,view.id())).isEqualTo(view);
        }
    }
}

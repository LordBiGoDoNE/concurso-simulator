package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.AttemptUseCases;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

/** Deliberate synthetic fixture in a separate helper container, never an API/authentication provider. */
public final class LocalDeliveryFixture {
    public static void main(String[] args) throws Exception {
        if (args.length<1 || !Set.of("seed","session").contains(args[0])) throw new IllegalArgumentException("seed/session required");
        var password=Files.readString(Path.of("/run/secrets/db_password")).strip();
        var result=new LinkedHashMap<String,Object>();
        try(var db=new AttemptDatabase("jdbc:postgresql://postgres:5432/concurso_simulator","concurso",password)) {
            UUID user;
            if(args[0].equals("seed")) {
                assertThat(db.jdbc.queryForObject("select count(*) from app_user",Integer.class)).isZero();
                var json=SubjectReviewOperationalFixture.JSON;
                var fixture=json.readTree(LocalDeliveryFixture.class.getResourceAsStream("/mockexam/v5-creation.json"));
                var snapshot=fixture.get("snapshot");var item=snapshot.get("items").get(0);var source=item.get("question");
                var owner=json.treeToValue(snapshot.get("owner"),AttemptOwner.class);user=owner.id();
                var legacyId=UUID.fromString(snapshot.get("id").asString());
                var question=Question.draft(UUID.fromString(source.get("questionId").asString()),new QuestionOrigin("local-delivery","v5-capture"),UUID.fromString(source.get("revisionId").asString()),json.treeToValue(source.get("content"),QuestionContent.class));
                db.jdbc.update("insert into app_user(id) values (?)",user);
                db.jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com','local-synthetic-only')",user);
                db.questions.transaction(s->{s.insert(question);return null;});
                db.decide(question.id(),EditorialDecision.Action.SUBMIT);db.decide(question.id(),EditorialDecision.Action.APPROVE);
                db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) values (?,'ACCOUNT',?,?,?,?::jsonb)",legacyId,user,user,Timestamp.from(NOW),snapshot.toString());
                db.jdbc.update("insert into me_item values (?,?,0,?,?,?::jsonb)",UUID.fromString(item.get("id").asString()),legacyId,question.id(),question.latest().id(),item.toString());
                db.jdbc.update("insert into me_command values (?,'ACCOUNT',?,?,?, ?,?::jsonb)",UUID.randomUUID(),user,UUID.fromString(fixture.get("operationId").asString()),legacyId,fixture.get("command").get("hash").asString(),fixture.get("command").get("outcome").toString());
                db.seed("Questão sintética local adicional",true);
                var cases=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(NOW.plusSeconds(1),ZoneOffset.UTC),UUID::randomUUID,()->new Random(1),false);
                cases.finish(owner,legacyId,UUID.randomUUID(),0);
                var fresh=cases.create(owner,UUID.randomUUID(),composition(1)).attempt();cases.finish(owner,fresh.id(),UUID.randomUUID(),0);
                var active=cases.create(owner,UUID.randomUUID(),composition(1)).attempt();
                var other=UUID.randomUUID();db.jdbc.update("insert into app_user(id) values (?)",other);
                result.put("legacyId",legacyId);result.put("finishedId",fresh.id());result.put("activeId",active.id());
                result.put("otherCookie",SubjectReviewOperationalFixture.session(db,other));
            } else {
                user=UUID.fromString(args[1]);
                assertThat(db.jdbc.queryForObject("select count(*) from app_user where id=?",Integer.class,user)).isEqualTo(1);
            }
            result.put("userId",user);
            result.put("cookie",SubjectReviewOperationalFixture.session(db,user));
            result.put("sameCookie",SubjectReviewOperationalFixture.session(db,user));
        }
        // Captured privately by the host harness; never print this report in public CI diagnostics.
        System.out.println("LOCAL_FIXTURE_JSON="+SubjectReviewOperationalFixture.JSON.writeValueAsString(result));
    }
}

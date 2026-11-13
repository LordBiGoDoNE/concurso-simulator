package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

/** Ensaio deliberado, sintético e isolado: duas execuções reais do JAR, sem banco do operador. */
public final class PracticeCapacityOperationalFixture {
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final HttpClient HTTP=HttpClient.newHttpClient();
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Informe JAR atual e diretório privado de relatório");
        var jar=Path.of(args[0]).toAbsolutePath();var output=Path.of(args[1]).toAbsolutePath();Files.createDirectories(output);
        try(var pg=new org.testcontainers.postgresql.PostgreSQLContainer("postgres:18.6")) {
            pg.start();try(var db=new AttemptDatabase(pg)) {
                db.migrate("15");db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());
                new LessonPracticePersistenceTests().seedPractice(db,30);
                var store=new JpaAttemptStore(db.entities,db.manager,db.pool);
                var oldCommand=UUID.randomUUID();var old=captureFive(store,oldCommand,4);
                var legacy=captureFive(store,UUID.randomUUID(),3);
                var previousLarge=capture(store,UUID.randomUUID(),4,25);
                var originalPayload=db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,old.id());
                var p=LessonPracticeUseCasesTests.practice(store,NOW);var fresh=p.createPractice(ACCOUNT,UUID.randomUUID(),Topic.P01).attempt();
                assertThat(fresh.items()).hasSize(5);
                for(int n=0;n<2;n++) {
                    var item=fresh.items().get(n);p.answer(ACCOUNT,fresh.id(),UUID.randomUUID(),n,item.id(),item.alternatives().getFirst().id());
                }
                var cookie=SubjectReviewOperationalFixture.session(db,ACCOUNT.id());
                String captured;long firstPid;
                try(var first=SubjectReviewOperationalFixture.runtime(jar,db,pg,output.resolve("first-private.log"))) {
                    firstPid=first.process().pid();captured=request(first.port(),cookie,"GET","/api/v1/lesson-practices/"+fresh.id(),null,200);
                    assertThat(captured).doesNotContain("\"correctAlternativeId\"","\"resolution\"");
                    assertThat(JSON.readTree(captured).has("roleId")).isFalse();
                    var availability=JSON.readTree(request(first.port(),cookie,"GET","/api/v1/lesson-practices/availability?topicId=P01",null,200));
                    assertThat(availability.get("available").asInt()).isEqualTo(30);assertThat(availability.get("maximum").asInt()).isEqualTo(5);
                }
                try(var second=SubjectReviewOperationalFixture.runtime(jar,db,pg,output.resolve("restarted-private.log"))) {
                    assertThat(second.process().pid()).isNotEqualTo(firstPid);
                    assertThat(request(second.port(),cookie,"GET","/api/v1/lesson-practices/"+fresh.id(),null,200)).isEqualTo(captured);
                    var replay=JSON.readTree(request(second.port(),cookie,"POST","/api/v1/lesson-practices",JSON.writeValueAsString(Map.of("commandId",oldCommand,"topicId","P01")),200));
                    assertThat(replay.get("id").asString()).isEqualTo(old.id().toString());assertThat(replay.get("items").size()).isEqualTo(5);
                    assertThat(db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,old.id())).isEqualTo(originalPayload);
                    assertThat(JSON.readTree(request(second.port(),cookie,"GET","/api/v1/lesson-practices/"+legacy.id(),null,200)).get("items").size()).isEqualTo(5);
                    assertThat(JSON.readTree(request(second.port(),cookie,"GET","/api/v1/lesson-practices/"+previousLarge.id(),null,200)).get("items").size()).isEqualTo(25);
                    for(int n=2;n<5;n++) {
                        var item=fresh.items().get(n);
                        request(second.port(),cookie,"PUT","/api/v1/lesson-practices/"+fresh.id()+"/answers/"+item.id(),
                                JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),"expectedVersion",n,"alternativeId",item.alternatives().getFirst().id())),200);
                    }
                    var body=JSON.writeValueAsString(Map.of("commandId",UUID.randomUUID(),"expectedVersion",5));
                    var finished=request(second.port(),cookie,"POST","/api/v1/lesson-practices/"+fresh.id()+"/finish",body,200);
                    assertThat(request(second.port(),cookie,"POST","/api/v1/lesson-practices/"+fresh.id()+"/finish",body,200)).isEqualTo(finished);
                    var result=JSON.readTree(finished).get("result");assertThat(result.get("total").asInt()).isEqualTo(5);assertThat(result.get("blank").asInt()).isZero();
                    var history=JSON.readTree(request(second.port(),cookie,"GET","/api/v1/me/lesson-practices",null,200));
                    assertThat(history.get("items").valueStream().map(i->i.get("total").asInt()).toList()).contains(5,25);
                }
                var report=Map.of("status","passed","separateJarProcesses",2,"newCaptureItems",5,"resumedAnswers",2,
                        "completedAnswers",5,"oldCommandItems",5,"historicalFormats",List.of(3,4),"schemaVersion",15,
                        "jarSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
                Files.writeString(output.resolve("report.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
                System.out.println("PRACTICE_CAPACITY_PASS: sorteio de cinco, reinício real e captura anterior de 25 preservada");
            }
        }
    }
    static Attempt.Snapshot captureFive(AttemptStore store,UUID command,int format) {
        return capture(store,command,format,5);
    }
    static Attempt.Snapshot capture(AttemptStore store,UUID command,int format,int count) {
        return store.transaction(s->{
            var items=s.eligible(QuestionPurpose.PRATICA_AULA).subList(0,count).stream().map(TopicPracticeDomainTests::item).toList();
            var composition=new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,Map.of(Topic.P01.discipline(),new Composition.Portion(count,Set.of(Topic.P01))));
            var snapshot=Attempt.restore(new Attempt.Snapshot(UUID.randomUUID(),ACCOUNT,format==3?composition:null,NOW,null,Attempt.POLICY,items,Map.of(),0,format,QuestionPurpose.PRATICA_AULA,Topic.P01)).snapshot();
            s.insert(snapshot);
            var hash=format==3?TopicPracticeCompatibilityTests.legacyHash("lesson-practice/v1",composition.role().name(),Topic.P01.name()):TopicPracticeCompatibilityTests.legacyHash("lesson-practice/v2",Topic.P01.name());
            s.record(ACCOUNT,command,new AttemptStore.Command(hash,snapshot));return snapshot;
        });
    }
    static String request(int port,String cookie,String method,String path,String body,int status) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10)).header("Cookie",cookie);
        if(body!=null) {
            var csrf=JSON.readTree(request(port,cookie,"GET","/api/v1/csrf",null,200));
            builder.header("X-CSRF-TOKEN",csrf.get("token").asString()).header("Content-Type","application/json");
        }
        var r=HTTP.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).as("%s %s",method,path).isEqualTo(status);return r.body();
    }
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.application.AttemptUseCases;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

/** Deliberate process/JAR check. Only disposable synthetic PostgreSQL; never in bootJar/startup. */
public final class SubjectReviewOperationalFixture {
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !Files.isRegularFile(Path.of(args[0])))throw new IllegalArgumentException("Informe o JAR atual compatível");
        var jar=Path.of(args[0]).toAbsolutePath();
        try(var archive=new ZipFile(jar.toFile())) {
            assertThat(archive.getEntry("BOOT-INF/classes/br/com/concursosimulator/subjectreview/web/ReviewController.class")).isNotNull();
            assertThat(archive.getEntry("BOOT-INF/classes/db/migration/V10__completed_subject_window_index.sql")).isNotNull();
        }
        var logs=Path.of("build/subject-review-ops").toAbsolutePath();Files.createDirectories(logs);
        try(var pg=new PostgreSQLContainer("postgres:18.6")) {
            pg.start();try(var db=new AttemptDatabase(pg,"5")) {
                var fixture=JSON.readTree(SubjectReviewOperationalFixture.class.getResourceAsStream("/mockexam/v5-creation.json"));
                assertThat(fixture.get("sourceCommit").asString()).isEqualTo("0d0fc4848f55fc93c736aa76dcfbb73a7269eb39");
                var snapshot=fixture.get("snapshot");var item=snapshot.get("items").get(0);var source=item.get("question");
                var owner=JSON.treeToValue(snapshot.get("owner"),AttemptOwner.class);var legacyId=UUID.fromString(snapshot.get("id").asString());
                var content=JSON.treeToValue(source.get("content"),QuestionContent.class);
                var question=Question.draft(UUID.fromString(source.get("questionId").asString()),new QuestionOrigin("v5-real-code","subject-ops"),UUID.fromString(source.get("revisionId").asString()),content);
                db.jdbc.update("insert into app_user(id) values (?)",owner.id());
                db.jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com','subject-ops-only')",owner.id());
                db.questions.transaction(s->{s.insert(question);return null;});db.decide(question.id(),EditorialDecision.Action.SUBMIT);db.decide(question.id(),EditorialDecision.Action.APPROVE);
                db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) values (?,'ACCOUNT',?,?,?,?::jsonb)",legacyId,owner.id(),owner.id(),Timestamp.from(NOW),snapshot.toString());
                db.jdbc.update("insert into me_item values (?,?,0,?,?,?::jsonb)",UUID.fromString(item.get("id").asString()),legacyId,question.id(),question.latest().id(),item.toString());
                db.jdbc.update("insert into me_command values (?,'ACCOUNT',?,?,?, ?,?::jsonb)",UUID.randomUUID(),owner.id(),UUID.fromString(fixture.get("operationId").asString()),legacyId,fixture.get("command").get("hash").asString(),fixture.get("command").get("outcome").toString());
                db.migrate("9");
                var cases=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(NOW.plusSeconds(1),ZoneOffset.UTC),UUID::randomUUID,()->new Random(1),false);
                cases.finish(owner,legacyId,UUID.randomUUID(),0);
                var fresh=cases.create(owner,UUID.randomUUID(),composition(1)).attempt();cases.finish(owner,fresh.id(),UUID.randomUUID(),0);
                assertThat(db.jdbc.queryForList("select coalesce(payload->>'format','1') from me_attempt order by 1",String.class)).containsExactly("1","2");
                var frozen=rows(db);var cookie=session(db,owner.id());
                Map<String,String> expected;long firstPid;
                try(var first=runtime(jar,db,pg,logs.resolve("upgrade-private.log"))) {
                    firstPid=first.process.pid();expected=read(first.port,cookie,legacyId,fresh.id());
                    var summary=JSON.readTree(expected.get("/api/v1/me/subject-review?roleId=assistente-administrativo"));
                    assertThat(summary.get("window").get("attemptCount").asInt()).isEqualTo(2);
                    int assessed=0,exposures=0;for(var metric:summary.get("subjects")){assessed+=metric.get("assessed").asInt();exposures+=metric.get("exposures").asInt();}
                    assertThat(assessed).isEqualTo(1);assertThat(exposures).isEqualTo(2);
                    assertThat(db.jdbc.queryForObject("select count(*) from pg_indexes where schemaname=? and indexname='me_completed_subject_window_idx'",Integer.class,db.schema)).isEqualTo(1);
                    assertThat(rows(db)).isEqualTo(frozen);
                }
                // Compatible recovery retains V10 and reads both formats with generation disabled.
                // A V5 binary is never launched once format 2 exists; no downgrade/DDL rollback.
                try(var restarted=runtime(jar,db,pg,logs.resolve("compatible-restart-private.log"))) {
                    assertThat(restarted.process.pid()).isNotEqualTo(firstPid);
                    assertThat(read(restarted.port,cookie,legacyId,fresh.id())).isEqualTo(expected);assertThat(rows(db)).isEqualTo(frozen);
                }
                var report=new LinkedHashMap<String,Object>();report.put("status","passed");report.put("upgrade","V9-to-V10");report.put("legacySourceCommit",fixture.get("sourceCommit").asString());
                report.put("jarSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
                report.put("separateProcesses",2);report.put("formats",List.of(1,2));report.put("businessRowsUnchanged",true);report.put("generationEnabled",false);
                report.put("limitations",List.of("Synthetic fixed captures; generated captures covered by SubjectReviewCaptureReadTests.","Session access timestamps may change; business/editorial/capture/command tables compared in full.","Recovery uses the current compatible binary, preserving V10; no V5 binary or destructive schema downgrade."));
                Files.writeString(logs.resolve("report.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report));
                System.out.println("SUBJECT_REVIEW_OPERATIONAL_PASS: V9/V10, V5+format2, two JAR processes, compatible recovery, unchanged business rows");
            }
        }
    }
    static List<List<String>> rows(AttemptDatabase db) {
        return List.of("app_user","external_identity","qb_question","qb_revision","qb_decision","pq_family","pq_version","pq_decision","pq_instance","me_attempt","me_item","me_command").stream()
                .map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
    }
    @SuppressWarnings({"unchecked","rawtypes"}) static String session(AttemptDatabase db,UUID user) {
        org.springframework.session.SessionRepository<org.springframework.session.Session> sessions=(org.springframework.session.SessionRepository)new JdbcIndexedSessionRepository(db.jdbc,new TransactionTemplate(new DataSourceTransactionManager(db.ds)));
        var session=sessions.createSession();session.setMaxInactiveInterval(Duration.ofMinutes(30));session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(user),null,List.of())));
        sessions.save(session);return "SESSION="+session.getId();
    }
    static Map<String,String> read(int port,String cookie,UUID old,UUID fresh) throws Exception {
        var responses=new LinkedHashMap<String,String>();
        for(var path:List.of("/api/v1/me/subject-review?roleId=assistente-administrativo","/api/v1/attempts/"+old+"/subject-review","/api/v1/attempts/"+fresh+"/subject-review","/api/v1/attempts/"+old+"/result","/api/v1/attempts/"+fresh+"/result")) {
            var response=HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Cookie",cookie).timeout(Duration.ofSeconds(20)).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(200);assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");responses.put(path,response.body());
        }
        return responses;
    }
    record Runtime(Process process,int port) implements AutoCloseable {
        public void close() throws Exception {process.destroy();if(!process.waitFor(10,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS);}}
    }
    static Runtime runtime(Path jar,AttemptDatabase db,PostgreSQLContainer pg,Path log) throws Exception {
        int port;try(var socket=new ServerSocket(0)){port=socket.getLocalPort();}
        var java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        var process=new ProcessBuilder(java.toString(),"-jar",jar.toString(),"--spring.profiles.active=local","--server.address=127.0.0.1","--server.port="+port,"--spring.datasource.url="+db.ds.getUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword(),"--parameterization.generation-enabled=false")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        var runtime=new Runtime(process,port);var deadline=Instant.now().plusSeconds(60);
        try {
            while(Instant.now().isBefore(deadline)&&process.isAlive()) {
                try {if(HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/status")).timeout(Duration.ofSeconds(2)).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200)return runtime;}
                catch(java.io.IOException ignored){}Thread.sleep(200);
            }
            throw new IllegalStateException("JAR atual não ficou pronto; consulte log privado do ensaio");
        }catch(Exception failure){runtime.close();throw failure;}
    }
}

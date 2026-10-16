package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.application.AttemptStore;
import br.com.concursosimulator.parameterization.domain.Family;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
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
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

/** Ensaio deliberado em PostgreSQL descartável; não entra no JAR nem no startup web. */
public final class ParameterizedOperationalFixture {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !Files.isRegularFile(Path.of(args[0])))throw new IllegalArgumentException("Informe JAR V5 aprovado");
        var oldJar=Path.of(args[0]).toAbsolutePath();var logs=Path.of("build/parameterized-ops").toAbsolutePath();Files.createDirectories(logs);
        var jarHash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(oldJar)));
        try(var pg=new PostgreSQLContainer("postgres:18.6")) {
            pg.start();try(var db=new ParameterDatabase(pg,"5")) {
                var definition=db.seed(Family.PERCENTAGE);var user=UUID.randomUUID();
                db.jdbc.update("insert into app_user(id) values (?)",user);
                db.jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com','parameterized-ops-only')",user);
                var cookie=session(db,user);var oldOperation=UUID.randomUUID();var oldRequest=create(oldOperation,false);
                String original,legacyId,saved;
                try(var old=oldRuntime(oldJar,db,pg,logs.resolve("v5-before-upgrade-private.log"))) {
                    original=call(old.port,"POST","/api/v1/attempts",cookie,oldRequest,201);legacyId=JSON.readTree(original).get("id").asString();
                    var item=JSON.readTree(original).get("items").get(0);var answer="{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":\""+item.get("alternatives").get(0).get("id").asString()+"\"}";
                    saved=call(old.port,"PUT","/api/v1/attempts/"+legacyId+"/answers/"+item.get("id").asString(),cookie,answer,200);
                }
                var tables=List.of("app_user","external_identity","spring_session","spring_session_attributes","qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command");
                var rows=tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
                var oldItem=db.jdbc.queryForObject("select payload::text from me_item where attempt_id=?",String.class,UUID.fromString(legacyId));
                var oldHash=db.jdbc.queryForObject("select payload_hash from me_command where operation_id=?",String.class,oldOperation);
                db.factory.close();db.factory=null;db.migrate("9");db.open(true);
                assertThat(tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList()).isEqualTo(rows);
                try(var old=oldRuntime(oldJar,db,pg,logs.resolve("v5-on-v9-private.log"))) {
                    assertThat(JSON.readTree(call(old.port,"GET","/api/v1/attempts/"+legacyId,cookie,null,200))).isEqualTo(JSON.readTree(saved));
                    assertThat(JSON.readTree(call(old.port,"POST","/api/v1/attempts",cookie,oldRequest,200))).isEqualTo(JSON.readTree(original));
                }
                db.approve(definition);var generatedOperation=UUID.randomUUID();var generatedRequest=create(generatedOperation,true);
                String generatedId,fixedId;UUID itemId,correctId;String expectedResolution;
                try(var app=current(db,pg,true)) {
                    int port=port(app);
                    assertThat(JSON.readTree(call(port,"POST","/api/v1/attempts",cookie,oldRequest,200))).isEqualTo(JSON.readTree(original));
                    fixedId=JSON.readTree(call(port,"POST","/api/v1/attempts",cookie,create(UUID.randomUUID(),false),201)).get("id").asString();
                    assertThat(db.jdbc.queryForObject("select payload->>'format' from me_attempt where id=?",String.class,UUID.fromString(fixedId))).isEqualTo("2");
                    // A primeira captura nova fixa já impede lançar V5. O processo não é iniciado.
                    assertThatThrownBy(()->oldRuntime(oldJar,db,pg,logs.resolve("must-not-start.log"))).isInstanceOf(IllegalStateException.class);
                    var response=call(port,"POST","/api/v1/attempts",cookie,generatedRequest,201);generatedId=JSON.readTree(response).get("id").asString();
                    var snapshot=app.getBean(AttemptStore.class).transaction(s->s.find(UUID.fromString(generatedId)).orElseThrow());
                    var item=snapshot.items().getFirst();assertThat(snapshot.format()).isEqualTo(2);assertThat(item.parameters()).isNotEmpty();
                    itemId=item.id();correctId=item.question().content().correctAlternativeId();expectedResolution=item.question().content().resolution().text();
                }
                String result;
                try(var rollback=current(db,pg,false)) {
                    int port=port(rollback);var freshCookie=session(db,user);
                    call(port,"GET","/api/v1/attempts/"+legacyId,freshCookie,null,200);
                    call(port,"GET","/api/v1/attempts/"+fixedId,freshCookie,null,200);
                    call(port,"GET","/api/v1/attempts/"+generatedId,freshCookie,null,200);
                    call(port,"POST","/api/v1/attempts",freshCookie,generatedRequest,200);
                    var before=db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class);
                    assertThat(call(port,"POST","/api/v1/attempts",freshCookie,create(UUID.randomUUID(),true),500)).isEqualTo("{\"error\":\"internal_error\"}");
                    assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(before);
                    call(port,"PUT","/api/v1/attempts/"+generatedId+"/answers/"+itemId,freshCookie,
                            "{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":\""+correctId+"\"}",200);
                    result=call(port,"POST","/api/v1/attempts/"+generatedId+"/finish",freshCookie,finish(1),200);
                    assertThat(JSON.readTree(result).get("result").get("correct").asInt()).isEqualTo(1);
                    assertThat(JSON.readTree(result).get("result").get("items").get(0).get("resolution").get("text").asString()).isEqualTo(expectedResolution);
                    call(port,"POST","/api/v1/attempts/"+legacyId+"/finish",freshCookie,finish(1),200);
                    call(port,"POST","/api/v1/attempts",freshCookie,create(UUID.randomUUID(),false),201);
                    assertThat(call(port,"GET","/api/v1/me/attempts",freshCookie,null,200)).contains(generatedId,legacyId);
                }
                try(var restarted=current(db,pg,false)) {
                    assertThat(JSON.readTree(call(port(restarted),"GET","/api/v1/attempts/"+generatedId+"/result",session(db,user),null,200)))
                            .isEqualTo(JSON.readTree(result).get("result"));
                }
                assertThat(db.jdbc.queryForObject("select payload::text from me_item where attempt_id=?",String.class,UUID.fromString(legacyId))).isEqualTo(oldItem);
                assertThat(db.jdbc.queryForObject("select payload_hash from me_command where operation_id=?",String.class,oldOperation)).isEqualTo(oldHash);
                assertThat(db.jdbc.queryForObject("select count(*) from me_command where attempt_id=? and jsonb_exists(outcome,'format')",Integer.class,UUID.fromString(legacyId))).isZero();
                assertThat(db.jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(1);
                assertThat(db.jdbc.queryForObject("select count(*) from external_identity",Integer.class)).isEqualTo(1);
                assertThat(db.jdbc.queryForObject("select count(*) from qb_revision",Integer.class)).isEqualTo(1);
                assertThat(db.jdbc.queryForObject("select count(*) from pq_instance",Integer.class)).isEqualTo(1);
                var report=Map.of("status","passed","previousJarSha256",jarHash,"upgrade","V5-to-V9",
                        "v5OnAdditiveSchemaBeforeNewWrite",true,"v5BlockedAfterNewFixedWrite",true,"compatibleRollback",true,
                        "legacyJsonAndHashPreserved",true,"generatedResultAfterRestart",true,"data","isolated-synthetic-only");
                Files.writeString(logs.resolve("result.json"),JSON.writeValueAsString(report)+"\n");System.out.println("PARAMETERIZED_OPERATIONAL_PASS "+JSON.writeValueAsString(report));
            }
        }
    }
    private static ConfigurableApplicationContext current(ParameterDatabase db,PostgreSQLContainer pg,boolean enabled) {
        return new SpringApplicationBuilder(ConcursoSimulatorApplication.class).run("--spring.profiles.active=local","--server.address=127.0.0.1","--server.port=0",
                "--spring.datasource.url="+db.ds.getUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword(),
                "--parameterization.generation-enabled="+enabled);
    }
    private static int port(ConfigurableApplicationContext app){return app.getEnvironment().getProperty("local.server.port",Integer.class);}
    @SuppressWarnings({"unchecked","rawtypes"}) private static String session(ParameterDatabase db,UUID user) {
        org.springframework.session.SessionRepository<org.springframework.session.Session> sessions=(org.springframework.session.SessionRepository)
                new JdbcIndexedSessionRepository(db.jdbc,new TransactionTemplate(new DataSourceTransactionManager(db.ds)));
        var session=sessions.createSession();session.setMaxInactiveInterval(Duration.ofMinutes(30));session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(user),null,List.of())));sessions.save(session);return "SESSION="+session.getId();
    }
    private static String create(UUID command,boolean generated) {
        return "{\"commandId\":\""+command+"\",\"roleId\":\"assistente-administrativo\",\"disciplines\":[{\"disciplineId\":\"matematica-raciocinio\",\"quantity\":1}]"+(generated?",\"useParameterizedQuestions\":true":"")+"}";
    }
    private static String finish(int version){return "{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":"+version+"}";}
    private static String call(int port,String method,String path,String cookie,String body,int expected) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Cookie",cookie).timeout(Duration.ofSeconds(10));
        if(!method.equals("GET")) {
            var csrf=JSON.readTree(call(port,"GET","/api/v1/csrf",cookie,null,200));builder.header("X-CSRF-TOKEN",csrf.get("token").asString());
        }
        if(body!=null)builder.header("Content-Type","application/json");
        var response=HTTP.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method+" "+path).isEqualTo(expected);
        if(path.startsWith("/api/v1/attempts"))assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");return response.body();
    }
    private record OldRuntime(Process process,int port) implements AutoCloseable {
        @Override public void close() throws Exception {process.destroy();if(!process.waitFor(10,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS);}}
    }
    private static OldRuntime oldRuntime(Path jar,ParameterDatabase db,PostgreSQLContainer pg,Path log) throws Exception {
        if(db.jdbc.queryForObject("select count(*) from me_attempt where payload ? 'format'",Integer.class)!=0)
            throw new IllegalStateException("JAR V5 proibido após primeira escrita do formato novo");
        int port;try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}
        var java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        var process=new ProcessBuilder(java.toString(),"-jar",jar.toString(),"--spring.profiles.active=local","--server.address=127.0.0.1","--server.port="+port,
                "--spring.datasource.url="+db.ds.getUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        var runtime=new OldRuntime(process,port);var deadline=Instant.now().plusSeconds(60);
        try {
            while(Instant.now().isBefore(deadline)&&process.isAlive()) {
                try { if(HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/status")).timeout(Duration.ofSeconds(2)).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200)return runtime; }
                catch(java.io.IOException ignored){}Thread.sleep(200);
            }
            throw new IllegalStateException("JAR anterior não ficou pronto; consulte o log privado");
        } catch(Exception failure) {runtime.close();throw failure;}
    }
}

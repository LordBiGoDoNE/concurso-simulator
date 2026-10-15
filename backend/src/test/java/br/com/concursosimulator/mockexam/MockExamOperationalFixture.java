package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.application.AttemptUseCases;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static br.com.concursosimulator.mockexam.AttemptFixtures.composition;
import static org.assertj.core.api.Assertions.*;

/** Verificação operacional deliberada; sem seeds/rotas/artefatos no JAR de produção. */
public final class MockExamOperationalFixture {
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void main(String[] args) throws Exception {
        if (args.length!=1 || !Files.isRegularFile(Path.of(args[0]))) throw new IllegalArgumentException("Informe o JAR anterior aprovado");
        var oldJar=Path.of(args[0]).toAbsolutePath();var logs=Path.of("build/mock-ops").toAbsolutePath();Files.createDirectories(logs);
        try(var pg=new PostgreSQLContainer("postgres:18.6")) {
            pg.start();
            try(var db=new AttemptDatabase(pg)) {
                var owner=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
                var guest=new AttemptOwner(AttemptOwner.Kind.VISITOR,UUID.randomUUID());
                db.jdbc.update("insert into app_user(id) values (?)",owner.id());
                db.jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com','operational-fixture')",owner.id());
                db.seed("Questão sintética operacional",true);
                var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                var cases=cases(db,now);
                var active=cases.create(owner,UUID.randomUUID(),composition(1)).attempt();var item=active.items().getFirst();
                var saved=cases.answer(owner,active.id(),UUID.randomUUID(),0,item.id(),item.alternatives().getFirst().id()).attempt();
                var completed=cases.create(owner,UUID.randomUUID(),composition(1)).attempt();
                var grade=cases.finish(owner,completed.id(),UUID.randomUUID(),0).result();
                var alive=cases.create(guest,UUID.randomUUID(),composition(1)).attempt();
                var expired=cases(db,now.minus(Duration.ofHours(25))).create(guest,UUID.randomUUID(),composition(1)).attempt();
                org.springframework.session.SessionRepository<org.springframework.session.Session> sessions=(org.springframework.session.SessionRepository)
                        new JdbcIndexedSessionRepository(db.jdbc,new TransactionTemplate(new DataSourceTransactionManager(db.ds)));
                var session=sessions.createSession();session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(owner.id()),null,List.of())));
                sessions.save(session);var cookie="SESSION="+session.getId();
                var before=count(db,"me_attempt");assertThat(before).isEqualTo(4);
                int port;try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}
                var java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
                var process=new ProcessBuilder(java,"-jar",oldJar.toString(),"--server.address=127.0.0.1","--server.port="+port,
                        "--spring.profiles.active=local","--spring.datasource.url="+db.ds.getUrl(),
                        "--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword())
                        .redirectErrorStream(true).redirectOutput(logs.resolve("previous-runtime-private.log").toFile()).start();
                try {
                    var deadline=Instant.now().plusSeconds(45);boolean ready=false;
                    while(Instant.now().isBefore(deadline)&&process.isAlive()) {
                        try { if(get(port,"/api/v1/status",null).statusCode()==200){ready=true;break;} } catch(Exception unavailable) { /* aguarda startup isolado */ }
                        Thread.sleep(100);
                    }
                    assertThat(ready).as("JAR anterior inicializa sobre V5 preservado").isTrue();
                    assertThat(get(port,"/api/v1/me",cookie).body()).contains(owner.id().toString());
                    assertThat(get(port,"/api/v1/attempts/"+active.id(),cookie).statusCode()).isEqualTo(403);
                    assertThat(get(port,"/api/v1/me/attempts",cookie).statusCode()).isEqualTo(403);
                    assertThat(get(port,"/api/v1/question-bank/catalog",null).statusCode()).isEqualTo(200);
                    assertThat(count(db,"me_attempt")).isEqualTo(before);assertThat(count(db,"app_user")).isEqualTo(1);
                    assertThat(count(db,"external_identity")).isEqualTo(1);assertThat(count(db,"qb_revision")).isEqualTo(1);
                } finally { process.destroy();if(!process.waitFor(10,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS);} }
                try(var current=new SpringApplicationBuilder(ConcursoSimulatorApplication.class).run("--server.address=127.0.0.1","--server.port=0",
                        "--spring.profiles.active=local","--spring.datasource.url="+db.ds.getUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword())) {
                    var fresh=current.getBean(AttemptUseCases.class);
                    assertThat(fresh.read(owner,active.id())).isEqualTo(saved);assertThat(fresh.result(owner,completed.id())).isEqualTo(grade);
                    assertThat(fresh.read(guest,alive.id()).id()).isEqualTo(alive.id());
                    assertThatThrownBy(() -> fresh.read(guest,expired.id())).isInstanceOf(Attempt.Unavailable.class);
                    assertThat(count(db,"me_attempt")).isEqualTo(3);
                    for(var table:List.of("me_item","me_command")) assertThat(db.jdbc.queryForObject("select count(*) from "+table+" where attempt_id=?",Integer.class,expired.id())).isZero();
                    var currentPort=current.getEnvironment().getProperty("local.server.port",Integer.class);
                    assertThat(get(currentPort,"/api/v1/me",cookie).statusCode()).isEqualTo(200);
                    assertThat(get(currentPort,"/api/v1/attempts/"+active.id(),cookie).statusCode()).isEqualTo(200);
                    assertThat(count(db,"app_user")).isEqualTo(1);assertThat(count(db,"qb_revision")).isEqualTo(1);
                }
                System.out.println("OPERATIONAL_PASS: previous JAR/V5 rollback, identity/session/bank preserved, restart/resume/result and expired visitor cascade verified");
            }
        }
    }
    private static AttemptUseCases cases(AttemptDatabase db,Instant now) {
        return new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(now,ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42));
    }
    private static int count(AttemptDatabase db,String table) { return db.jdbc.queryForObject("select count(*) from "+table,Integer.class); }
    private static HttpResponse<String> get(int port,String path,String cookie) throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(3));if(cookie!=null)r.header("Cookie",cookie);
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build().send(r.build(),HttpResponse.BodyHandlers.ofString());
    }
}

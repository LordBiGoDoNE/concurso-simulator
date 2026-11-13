package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.application.AttemptStore;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.questionbank.domain.*;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;

/** Somente teste/manual isolado da #161, sem fixtures nem rotas no bootJar de produção. */
public final class TopicPracticeBrowserFixture {
    /** Portas ui, api e controle; TOPIC_PRACTICE_PORTS="ui,api,controle" evita ambientes locais em uso. */
    static int[] ports(boolean expansion){
        var configured=System.getenv("TOPIC_PRACTICE_PORTS");
        if(configured==null || configured.isBlank())return expansion?new int[]{5393,19370,19371}:new int[]{5371,19160,19161};
        var p=configured.split(",");
        if(p.length!=3)throw new IllegalArgumentException("TOPIC_PRACTICE_PORTS=ui,api,controle");
        return new int[]{Integer.parseInt(p[0].trim()),Integer.parseInt(p[1].trim()),Integer.parseInt(p[2].trim())};
    }
    public static void main(String[] args) throws Exception {
        boolean restoring=args.length==3 && args[0].equals("--restore");
        if(args.length!=0 && !restoring)throw new IllegalArgumentException("Uso: --restore dump-local sha256");
        boolean expansion="1".equals(System.getenv("PRACTICE_EXPANSION"));
        int[] chosen=ports(expansion);
        int uiPort=chosen[0], apiPort=chosen[1], controlPort=chosen[2];
        var pg=new PostgreSQLContainer("postgres:18.6");pg.start();
        ConfigurableApplicationContext app=null;HttpServer control=null;
        try {
            if(restoring) {
                var dump=java.nio.file.Path.of(args[1]).toAbsolutePath().normalize();
                if(!java.nio.file.Files.isRegularFile(dump) || java.nio.file.Files.size(dump)>67108864
                        || !HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(dump))).equals(args[2]))
                    throw new IllegalArgumentException("Dump local ausente, grande ou hash divergente");
                pg.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(dump.toString()),"/tmp/practice-local-restore.dump");
                var restored=pg.execInContainer("pg_restore","-U",pg.getUsername(),"-d",pg.getDatabaseName(),"--exit-on-error","--no-owner","--no-privileges","/tmp/practice-local-restore.dump");
                if(restored.getExitCode()!=0)throw new IllegalStateException("Restauração do ambiente sintético falhou");
                pg.execInContainer("rm","--","/tmp/practice-local-restore.dump");
            }
            app=new SpringApplicationBuilder(ConcursoSimulatorApplication.class).run(
                    "--spring.profiles.active=local","--server.port="+apiPort,"--server.address=127.0.0.1",
                    "--spring.datasource.url="+pg.getJdbcUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword(),
                    "--app.cors-origins=http://127.0.0.1:"+uiPort,"--spring.session.timeout=30m","--app.google.enabled=false");
            if(!restoring)br.com.concursosimulator.questionbank.QuestionBrowserFixtures.seed(app);
            try(var db=new AttemptDatabase(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword())) {
                for(int i=0;!restoring && i<30;i++) {
                    var c=TopicPracticeDomainTests.classified(i,i%2==0?TargetRole.ADMINISTRATIVE_ASSISTANT:TargetRole.SCHOOL_SECRETARY,Topic.P01).content();
                    var q=Question.draft(UUID.randomUUID(),new QuestionOrigin(QuestionPurpose.PRACTICE_NAMESPACE,"topic-browser-"+i),UUID.randomUUID(),c);
                    db.questions.transaction(s->{s.insert(q);return null;});
                    db.decide(q.id(),EditorialDecision.Action.SUBMIT);db.decide(q.id(),EditorialDecision.Action.APPROVE);
                }
                var repository=repository(app);var issued=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
                var jdbc=app.getBean(org.springframework.jdbc.core.JdbcTemplate.class);var store=app.getBean(AttemptStore.class);
                control=HttpServer.create(new InetSocketAddress("127.0.0.1",controlPort),0);
                var json=JsonMapper.builder().build();
                control.createContext("/fixture",exchange->{
                    if(!exchange.getRequestMethod().equals("GET")) { exchange.sendResponseHeaders(405,-1);exchange.close();return; }
                    var id=UUID.randomUUID();var other=UUID.randomUUID();jdbc.update("insert into app_user(id) values (?)",id);jdbc.update("insert into app_user(id) values (?)",other);
                    var old=TopicPracticeCompatibilityTests.legacy(store,new AttemptOwner(AttemptOwner.Kind.ACCOUNT,id),UUID.randomUUID());
                    var cookie=session(repository,id);var otherCookie=session(repository,other);issued.add(cookie);issued.add(otherCookie);
                    var bytes=json.writeValueAsBytes(Map.of("cookie",cookie,"otherCookie",otherCookie,"legacyId",old.id(),"accountId",id));
                    exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
                });
                control.createContext("/expire",exchange->{
                    if(!exchange.getRequestMethod().equals("POST")) { exchange.sendResponseHeaders(405,-1);exchange.close();return; }
                    var cookie=exchange.getRequestHeaders().getFirst("X-Fixture-Session");
                    var s=cookie!=null && issued.contains(cookie)?repository.findById(cookie):null;if(s!=null){s.setLastAccessedTime(Instant.now().minusSeconds(7200));repository.save(s);}
                    exchange.sendResponseHeaders(204,-1);exchange.close();
                });
                control.start();
            }
        } catch(Exception failure) { if(control!=null)control.stop(0);if(app!=null)app.close();pg.stop();throw failure; }
        var running=app;var server=control;
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(0);running.close();pg.stop();}));
        System.out.println("TOPIC_PRACTICE_READY: UI "+uiPort+" / API "+apiPort+" / fixture "+controlPort+" / PostgreSQL efêmero");
        new java.util.concurrent.CountDownLatch(1).await();
    }
    @SuppressWarnings({"unchecked","rawtypes"}) static SessionRepository<Session> repository(ConfigurableApplicationContext app) {
        return (SessionRepository)app.getBean(JdbcIndexedSessionRepository.class);
    }
    static String session(SessionRepository<Session> repository,UUID id) {
        var s=repository.createSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(id),null,List.of())));repository.save(s);return s.getId();
    }
}

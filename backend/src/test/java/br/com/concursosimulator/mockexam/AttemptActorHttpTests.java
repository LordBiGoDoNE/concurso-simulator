package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.mockexam.infrastructure.AttemptAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;
import br.com.concursosimulator.mockexam.web.CurrentActor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("local") @Testcontainers
@Import(AttemptActorHttpTests.Probe.class)
class AttemptActorHttpTests {
    @RestController static class Probe {
        record Mode(String mode) {}
        @GetMapping("/api/v1/attempts/actor-probe") Mode probe(@CurrentActor AttemptOwner actor) { return new Mode(actor.kind().name()); }
    }
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",PG::getJdbcUrl); r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions;
    final HttpClient client=HttpClient.newHttpClient();
    final JsonMapper json=JsonMapper.builder().build();
    final String path="/api/v1/attempts/actor-probe";
    @Test void routeSecurityRejectsInvalidPrincipalsAndGuestHistoryWithoutRevealingState() throws Exception {
        for (Authentication auth:new Authentication[]{null,
                UsernamePasswordAuthenticationToken.authenticated("invalid",null,List.of()),
                UsernamePasswordAuthenticationToken.unauthenticated(new UserPrincipal(UUID.randomUUID()),null),
                new AnonymousAuthenticationToken("fixture",new UserPrincipal(UUID.randomUUID()),List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))}) {
            var cookie=session(auth); var response=send("GET",auth==null?"/api/v1/me/attempts":path,cookie,null);
            assertThat(response.statusCode()).isEqualTo(401); assertThat(response.body()).isEqualTo("{\"error\":\"unauthenticated\"}");
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store"); assertThat(response.headers().firstValue("Location")).isEmpty();
        }
    }
    @Test void visitorAndAccountPassTechnicalBoundaryWhileCsrfAndCorsRemainEnforced() throws Exception {
        var csrf=send("GET","/api/v1/csrf",null,null); var cookie=csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        var token=json.readTree(csrf.body()).get("token").asString();
        assertThat(send("POST","/api/v1/attempts",cookie,null).statusCode()).isEqualTo(403);
        assertThat(send("PUT",path+"/answers/"+UUID.randomUUID(),cookie,null).statusCode()).isEqualTo(403);
        var visitorResponse=send("GET",path,cookie,null);
        assertThat(visitorResponse.statusCode()).isEqualTo(200);assertThat(visitorResponse.body()).isEqualTo("{\"mode\":\"VISITOR\"}");
        var id=cookie.substring("SESSION=".length());
        UUID visitor=repository().findById(id).getAttribute(AttemptAccess.VISITOR); assertThat(visitor).isNotNull();
        assertThat(send("GET",path,cookie,null).statusCode()).isEqualTo(200);
        assertThat((UUID)repository().findById(id).getAttribute(AttemptAccess.VISITOR)).isEqualTo(visitor);
        var account=session(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(UUID.randomUUID()),null,List.of()));
        assertThat(send("GET",path,account,null).body()).isEqualTo("{\"mode\":\"ACCOUNT\"}");
        assertThat(send("PATCH",path,cookie,token).statusCode()).isEqualTo(403);
        var preflight=HttpRequest.newBuilder(URI.create(base()+"/api/v1/attempts")).method("OPTIONS",HttpRequest.BodyPublishers.noBody())
                .header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","X-CSRF-TOKEN");
        var allowed=client.send(preflight.build(),HttpResponse.BodyHandlers.ofString()); assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        assertThat(client.send(HttpRequest.newBuilder(URI.create(base()+path)).header("Origin","https://unknown.example").build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        var expired=repository().findById(id);expired.setLastAccessedTime(Instant.now().minusSeconds(3600));repository().save(expired);
        assertThat(send("GET","/api/v1/me/attempts",cookie,null).statusCode()).isEqualTo(401);
        assertThat(send("POST","/api/v1/attempts",cookie,token).statusCode()).isEqualTo(403);
    }
    @Test void concurrentInitialRequestsHaveSameOpaqueVisitorAndLoginNeverAdoptsIt() {
        var access=new AttemptAccess(); var sessionId=UUID.randomUUID().toString(); var owners=new ArrayList<AttemptOwner>();
        for (int i=0;i<2;i++) {
            var request=new MockHttpServletRequest();request.setSession(new MockHttpSession(null,sessionId));
            assertThat(access.authorize(null,request,false).isGranted()).isTrue();
            owners.add((AttemptOwner)request.getAttribute(AttemptAccess.ACTOR));
        }
        assertThat(owners.getFirst()).isEqualTo(owners.getLast()); assertThat(owners.getFirst().id().toString()).isNotEqualTo(sessionId);
        var request=new MockHttpServletRequest();request.getSession().setAttribute(AttemptAccess.VISITOR,owners.getFirst().id());
        var account=UUID.randomUUID(); access.authorize(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(account),null,List.of()),request,false);
        assertThat(request.getAttribute(AttemptAccess.ACTOR)).isEqualTo(new AttemptOwner(AttemptOwner.Kind.ACCOUNT,account));
    }
    @SuppressWarnings({"unchecked","rawtypes"}) SessionRepository<Session> repository() { return (SessionRepository)sessions; }
    String session(Authentication auth) {
        var s=repository().createSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(auth));repository().save(s);return "SESSION="+s.getId();
    }
    String base() { return "http://127.0.0.1:"+port; }
    HttpResponse<String> send(String method,String path,String cookie,String token) throws Exception {
        var r=HttpRequest.newBuilder(URI.create(base()+path)).timeout(Duration.ofSeconds(10)).method(method,HttpRequest.BodyPublishers.noBody());
        if (cookie!=null) r.header("Cookie",cookie);if (token!=null)r.header("X-CSRF-TOKEN",token);
        return client.send(r.build(),HttpResponse.BodyHandlers.ofString());
    }
}

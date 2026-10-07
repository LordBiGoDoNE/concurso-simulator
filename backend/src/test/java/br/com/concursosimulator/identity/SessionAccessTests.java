package br.com.concursosimulator.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class SessionAccessTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;
    final HttpClient client = HttpClient.newHttpClient();

    @Test
    void visitorAndCsrfContract() throws Exception {
        var config = send("GET", "/api/v1/auth/config", null, null, null);
        HttpContract.json("/api/v1/auth/config", 200, config);
        assertThat(config.body()).isEqualTo("{\"googleEnabled\":false}");
        assertThat(send("GET", "/oauth2/authorization/google", null, null, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/login/oauth2/code/google", null, null, null).statusCode()).isEqualTo(404);
        var me = send("GET", "/api/v1/me", null, null, null);
        HttpContract.json("/api/v1/me", 401, me);
        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(json(me)).containsExactly(Map.entry("error", "unauthenticated"));
        assertThat(me.headers().firstValue("Cache-Control")).contains("no-store");
        var csrf = send("GET", "/api/v1/csrf", null, null, null);
        HttpContract.json("/api/v1/csrf", 200, csrf);
        assertThat(csrf.statusCode()).isEqualTo(200);
        assertThat(json(csrf)).containsOnlyKeys("token", "headerName");
        assertThat(json(csrf).get("token")).isInstanceOf(String.class);
        assertThat(csrf.headers().firstValue("Cache-Control")).contains("no-store");
        String cookie = csrf.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).contains("HttpOnly", "SameSite=Lax", "Path=/").doesNotContain("Secure", "Domain=");
    }

    @Test
    void logoutRequiresCsrfAndOnlyInvalidatesCurrentSession() throws Exception {
        UUID id = UUID.randomUUID();
        String first = authenticatedSession(id);
        String second = authenticatedSession(id);
        HttpContract.json("/api/v1/me", 200, send("GET", "/api/v1/me", first, null, null));
        assertThat(send("GET", "/api/v1/me", first, null, null).body()).isEqualTo("{\"id\":\"" + id + "\"}");
        assertThat(send("POST", "/api/v1/auth/logout", first, null, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/me", first, null, null).statusCode()).isEqualTo(200);
        String token = json(send("GET", "/api/v1/csrf", first, null, null)).get("token").toString();
        assertThat(send("POST", "/api/v1/auth/logout", second, token, null).statusCode()).isEqualTo(403);
        var logout = send("POST", "/api/v1/auth/logout", first, token, null);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().allValues("Set-Cookie").toString()).contains("Max-Age=0");
        assertThat(send("GET", "/api/v1/me", first, null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/me", second, null, null).statusCode()).isEqualTo(200);
        var anonymous = send("GET", "/api/v1/csrf", null, null, null);
        var anonymousCookie = anonymous.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        assertThat(send("POST", "/api/v1/auth/logout", anonymousCookie, json(anonymous).get("token").toString(), null).statusCode()).isEqualTo(204);
    }

    @Test
    void expirationCleanupAndRepositoryRestart() throws Exception {
        String cookie = authenticatedSession(UUID.randomUUID());
        String id = cookie.substring("SESSION=".length());
        var recovered = new JdbcIndexedSessionRepository(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())));
        assertThat(recovered.findById(id)).isNotNull();
        Session recoveredSession = recovered.findById(id);
        assertThat(recoveredSession.getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
        var session = repository().findById(id);
        session.setLastAccessedTime(Instant.now().minus(Duration.ofHours(1)));
        repository().save(session);
        assertThat(send("GET", "/api/v1/me", cookie, null, null).statusCode()).isEqualTo(401);
        sessions.cleanUpExpiredSessions();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session WHERE session_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void corsCredentialsOnlyForKnownOriginsAndPrivateRoutesStayClosed() throws Exception {
        var allowed = send("GET", "/api/v1/me", null, null, "http://localhost:5173");
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        var denied = send("GET", "/api/v1/me", null, null, "https://unknown.example");
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(send("GET", "/api/v1/questions", authenticatedSession(UUID.randomUUID()), null, null).statusCode()).isEqualTo(403);
        var preflight = client.send(HttpRequest.newBuilder(URI.create(base() + "/api/v1/auth/logout"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-CSRF-TOKEN").build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(preflight.statusCode()).isEqualTo(200);
    }

    String authenticatedSession(UUID id) {
        // Apenas fixture de teste: nenhuma rota ou autenticação simulada em produção.
        var session = repository().createSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(id), null, java.util.List.of())));
        repository().save(session);
        return "SESSION=" + session.getId();
    }
    String base() { return "http://127.0.0.1:" + port; }
    @SuppressWarnings({"unchecked", "rawtypes"})
    SessionRepository<Session> repository() { return (SessionRepository) sessions; }
    HttpResponse<String> send(String method, String path, String cookie, String csrf, String origin) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) request.header("Cookie", cookie);
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        if (origin != null) request.header("Origin", origin);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    Map<String, Object> json(HttpResponse<String> response) { return new Yaml().load(response.body()); }
}

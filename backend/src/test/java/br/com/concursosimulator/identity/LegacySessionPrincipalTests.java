package br.com.concursosimulator.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.identity.application.UserPrincipal;
import br.com.concursosimulator.mockexam.infrastructure.AttemptAccess;
import br.com.concursosimulator.questionbank.QuestionBrowserFixtures;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sessões gravadas antes de {@code UserPrincipal} sair de {@code identity.web} (D7): ficam sem autenticação,
 * nunca 500, sem restaurar a identidade anterior nem criar conta. Os bytes antigos são reproduzidos trocando,
 * no fluxo de serialização Java, o nome da classe atual pelo nome antigo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class LegacySessionPrincipalTests {
    @Container
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    static final String LEGACY = "br.com.concursosimulator.identity.web.UserPrincipal";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcIndexedSessionRepository sessions;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ConfigurableApplicationContext context;

    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @Test
    void sessionWithRemovedPrincipalClassIsUnauthenticatedAndStudyContinuesAsVisitor() throws Exception {
        var user = UUID.randomUUID();
        var cookie = legacySession(user);
        int users = jdbc.queryForObject("select count(*) from app_user", Integer.class);

        var me = send("GET", "/api/v1/me", cookie);
        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(me.body()).doesNotContain(user.toString());
        // Estudo segue como visitante: a tentativa inexistente é 404 da aplicação, não 401/500.
        assertThat(send("GET", "/api/v1/attempts/" + UUID.randomUUID(), cookie).statusCode())
                .isEqualTo(404);
        assertThat(send("GET", "/api/v1/me/attempts", cookie).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select count(*) from app_user", Integer.class))
                .isEqualTo(users);
    }

    @Test
    void sessionWithRemovedPrincipalClassCreatesAttemptAsVisitor() throws Exception {
        if (jdbc.queryForObject("select count(*) from qb_question", Integer.class) == 0)
            QuestionBrowserFixtures.seed(context);
        var user = UUID.randomUUID();
        var cookie = legacySession(user);
        var sessionId = cookie.substring("SESSION=".length());
        int users = jdbc.queryForObject("select count(*) from app_user", Integer.class);

        var csrf = send("GET", "/api/v1/csrf", cookie, null, null);
        assertThat(csrf.statusCode()).isEqualTo(200);
        // A sessão antiga continua em uso (sem contexto de segurança); não é trocada por outra.
        assertThat(csrf.headers().firstValue("Set-Cookie")).isEmpty();
        var token = json.readTree(csrf.body()).get("token").asString();
        var body = "{\"commandId\":\"" + UUID.randomUUID() + "\",\"roleId\":\"assistente-administrativo\","
                + "\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":1,\"topicIds\":[]}]}";
        var created = send("POST", "/api/v1/attempts", cookie, token, body);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).doesNotContain(user.toString());
        var attempt = UUID.fromString(json.readTree(created.body()).get("id").asString());

        var owner = jdbc.queryForMap("select owner_kind, owner_id, account_id from me_attempt where id=?", attempt);
        @SuppressWarnings({"unchecked", "rawtypes"})
        var repository = (SessionRepository<Session>) (SessionRepository) sessions;
        UUID visitor = repository.findById(sessionId).getAttribute(AttemptAccess.VISITOR);
        assertThat(visitor).isNotNull().isNotEqualTo(user);
        assertThat(owner.get("owner_kind")).isEqualTo("VISITOR");
        assertThat(owner.get("owner_id")).isEqualTo(visitor);
        assertThat(owner.get("account_id")).isNull();
        assertThat(jdbc.queryForObject(
                        "select count(*) from me_attempt where owner_id=? or account_id=?", Integer.class, user, user))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from app_user", Integer.class))
                .isEqualTo(users);
        assertThat(jdbc.queryForObject("select count(*) from app_user where id=?", Integer.class, user))
                .isZero();
        // A identidade antiga não volta: a mesma sessão segue sem conta.
        assertThat(send("GET", "/api/v1/me", cookie).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/attempts/" + attempt, cookie).statusCode())
                .isEqualTo(200);
    }

    @Test
    void currentSessionsStillAuthenticate() throws Exception {
        var user = UUID.randomUUID();
        var me = send("GET", "/api/v1/me", session(user, false));
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(user.toString());
    }

    String legacySession(UUID user) throws Exception {
        return session(user, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    String session(UUID user, boolean legacy) throws Exception {
        var repository = (SessionRepository<Session>) (SessionRepository) sessions;
        var session = repository.createSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(
                        UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(user), null, List.of())));
        repository.save(session);
        if (legacy) {
            var primary = jdbc.queryForObject(
                    "select primary_id from spring_session where session_id=?", String.class, session.getId());
            var bytes = jdbc.queryForObject(
                    "select attribute_bytes from spring_session_attributes where session_primary_id=? and attribute_name=?",
                    byte[].class,
                    primary,
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            var patched = renameClass(bytes, UserPrincipal.class.getName(), LEGACY);
            assertThat(patched).isNotEqualTo(bytes);
            jdbc.update(
                    "update spring_session_attributes set attribute_bytes=? where session_primary_id=? and attribute_name=?",
                    patched,
                    primary,
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
        return "SESSION=" + session.getId();
    }

    /** Troca o descritor de classe (writeUTF: 2 bytes de tamanho + nome) no fluxo serializado. */
    static byte[] renameClass(byte[] stream, String from, String to) {
        var source = utf(from);
        var target = utf(to);
        var out = new ByteArrayOutputStream();
        int replaced = 0;
        for (int i = 0; i < stream.length; ) {
            if (i + source.length <= stream.length
                    && java.util.Arrays.equals(stream, i, i + source.length, source, 0, source.length)) {
                out.writeBytes(target);
                i += source.length;
                replaced++;
            } else out.write(stream[i++]);
        }
        assertThat(replaced).isEqualTo(1);
        return out.toByteArray();
    }

    static byte[] utf(String name) {
        var bytes = name.getBytes(StandardCharsets.UTF_8);
        var result = new byte[bytes.length + 2];
        result[0] = (byte) (bytes.length >> 8);
        result[1] = (byte) bytes.length;
        System.arraycopy(bytes, 0, result, 2, bytes.length);
        return result;
    }

    HttpResponse<String> send(String method, String path, String cookie) throws Exception {
        return send(method, path, cookie, null, null);
    }

    HttpResponse<String> send(String method, String path, String cookie, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .method(
                        method,
                        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Cookie", cookie);
        if (token != null) request.header("X-CSRF-TOKEN", token);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

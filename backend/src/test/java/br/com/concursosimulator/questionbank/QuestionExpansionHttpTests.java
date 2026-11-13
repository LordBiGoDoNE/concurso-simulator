package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.identity.web.UserPrincipal;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

/** Ensaio #173 com pacotes Python completos pela cadeia HTTP existente; sem acervo real. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Testcontainers
class QuestionExpansionHttpTests {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");
    static final UUID OPERATOR = UUID.randomUUID();
    static final Path FIXTURES = Path.of("build/question-expansion-http-fixtures");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername); r.add("spring.datasource.password", PG::getPassword);
        r.add("app.question-import.destination-id", () -> "isolated-expansion-173-http");
        r.add("app.question-import.confirmation-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;
    final JsonMapper json = JsonMapper.builder().build();
    final HttpClient http = HttpClient.newHttpClient();
    @BeforeAll static void generate() throws Exception { QuestionExpansionPackageTests.compileSyntheticPackages(FIXTURES); }
    @SuppressWarnings({"unchecked", "rawtypes"}) String operatorSession() {
        jdbc.update("insert into app_user(id) values (?)", OPERATOR);
        jdbc.update("update admin_registration set owner_id=?,must_change=false,enabled=true where id=1", OPERATOR);
        SessionRepository<Session> repository = (SessionRepository) sessions;
        var session = repository.createSession();
        var auth = UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(OPERATOR), null, List.of());
        auth.setDetails(new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence(
                jdbc.queryForObject("select auth_version from admin_registration where id=1", Long.class)));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(auth));
        repository.save(session);
        return "SESSION="+session.getId();
    }
    HttpResponse<String> request(String path, String cookie, String csrf, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/"+path))
                .timeout(Duration.ofSeconds(30));
        if (cookie != null) builder.header("Cookie", cookie);
        if (csrf != null) builder.header("X-CSRF-TOKEN", csrf);
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void compiledHundredItemPackagesPreviewApplyReplayAndStayDraftOverHttp() throws Exception {
        var cookie = operatorSession();
        var csrf = json.readTree(request("csrf", cookie, null, null).body()).path("token").asString();
        for (var purpose : List.of("SIMULADO", "PRATICA_AULA")) {
            var fixture = json.readTree(Files.readAllBytes(FIXTURES.resolve(purpose+".json")));
            var input = fixture.get("request"); var body = json.writeValueAsString(input);
            int before = jdbc.queryForObject("select count(*) from qb_question", Integer.class);
            assertThat(request("admin/question-imports/preview", cookie, null, body).statusCode()).isEqualTo(403);
            var preview = request("admin/question-imports/preview", cookie, csrf, body);
            assertThat(preview.statusCode()).as(preview.body()).isEqualTo(200);
            assertThat(preview.headers().firstValue("Cache-Control")).contains("no-store");
            var result = json.readTree(preview.body());
            assertThat(result.path("packageDigest").asString()).isEqualTo(fixture.path("digests").path("packageDigest").asString());
            assertThat(result.path("selectionDigest").asString()).isEqualTo(fixture.path("digests").path("selectionDigest").asString());
            assertThat(result.path("items").size()).isEqualTo(100);
            assertThat(result.path("confirmable").asBoolean()).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isEqualTo(before);
            var apply = json.createObjectNode();
            apply.set("package", input.get("package")); apply.set("keys", input.get("keys"));
            apply.put("confirmationToken", result.path("confirmationToken").asString());
            var applied = request("admin/question-imports/apply", cookie, csrf, json.writeValueAsString(apply));
            assertThat(applied.statusCode()).as(applied.body()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isEqualTo(before+100);
            var retry = request("admin/question-imports/apply", cookie, csrf, json.writeValueAsString(apply));
            assertThat(retry.statusCode()).as(retry.body()).isEqualTo(200);
            assertThat(json.readTree(retry.body()).path("replayed").asBoolean()).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from qb_revision where status <> 'DRAFT'", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from qb_question where eligible_revision_id is not null", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from qb_decision", Integer.class)).isZero();
            for (var response : List.of(preview, applied, retry))
                assertThat(response.body()).doesNotContain("correctAlternativeId", "Fonte sintética de teste", "Somar 2 e 2");
            var qid = json.readTree(applied.body()).path("receipt").path("results").get(0).path("questionId").asString();
            assertThat(request("question-bank/questions/"+qid, null, null, null).statusCode()).isEqualTo(404);
        }
        assertThat(jdbc.queryForObject("select count(*) from qb_import_receipt", Integer.class)).isEqualTo(2);
    }
}

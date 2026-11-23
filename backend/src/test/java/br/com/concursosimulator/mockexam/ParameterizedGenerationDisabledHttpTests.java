package br.com.concursosimulator.mockexam;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.questionbank.domain.Discipline;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import br.com.concursosimulator.shared.web.ErrorResponses;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** Geração parametrizada desligada: 503 nomeado, sem gravar tentativa ou comando (backend-http-boundaries, D4). */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "parameterization.generation-enabled=false")
@ActiveProfiles("local")
@Testcontainers
class ParameterizedGenerationDisabledHttpTests {
    @Container
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @Test
    void parameterizedCreationIsUnavailableWithoutEffects() throws Exception {
        var csrf = send("GET", "/api/v1/csrf", null, null, null);
        var cookie = csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        var token = json.readTree(csrf.body()).get("token").asString();
        var tables = List.of("me_attempt", "me_item", "me_command");
        var before = tables.stream()
                .map(t -> jdbc.queryForObject("select count(*) from " + t, Integer.class))
                .toList();
        var body = "{\"commandId\":\"" + UUID.randomUUID() + "\",\"roleId\":\""
                + TargetRole.ADMINISTRATIVE_ASSISTANT.id() + "\",\"disciplines\":[{\"disciplineId\":\""
                + Discipline.MATHEMATICS_LOGIC.id() + "\",\"quantity\":1}],\"useParameterizedQuestions\":true}";
        var response = send("POST", "/api/v1/attempts", cookie, token, body);
        ErrorResponses.assertError(response, 503, "PARAMETERIZED_GENERATION_UNAVAILABLE");
        assertThat(tables.stream()
                        .map(t -> jdbc.queryForObject("select count(*) from " + t, Integer.class))
                        .toList())
                .isEqualTo(before);
    }

    HttpResponse<String> send(String method, String path, String cookie, String csrf, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .method(
                        method,
                        body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) request.header("Cookie", cookie);
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        if (body != null) request.header("Content-Type", "application/json");
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

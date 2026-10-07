package br.com.concursosimulator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.flywaydb.core.Flyway;
import org.yaml.snakeyaml.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcursoSimulatorApplicationTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6")
            .withUrlParam("connectTimeout", "2").withUrlParam("socketTimeout", "3");

    @Value("${local.server.port}")
    int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Test
    @Order(1)
    void contextLoads() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM application_marker", Integer.class)).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM application_marker", Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(2)
    void statusMatchesOpenApi() throws Exception {
        assertContract(get("/api/v1/status", null), 200);
    }

    @Test
    @Order(3)
    void corsAndPrivateRoutes() throws Exception {
        var allowed = get("/api/v1/status", "http://localhost:5173");
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        var denied = get("/api/v1/status", "https://unknown.example");
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        assertThat(get("/api/v1/questions", null).statusCode()).isEqualTo(403);
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(403);
        var preflight = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/status"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").build(), HttpResponse.BodyHandlers.ofString());
        assertThat(preflight.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(99)
    void databaseLossReturnsDownWithinTimeout() throws Exception {
        POSTGRES.stop();
        long start = System.nanoTime();
        assertContract(get("/api/v1/status", null), 503);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(8));
    }

    HttpResponse<String> get(String path, String origin) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10)).GET();
        if (origin != null) request.header("Origin", origin);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    void assertContract(HttpResponse<String> response, int expectedCode) throws Exception {
        Map<String, Object> api = new Yaml().load(Files.readString(Path.of("openapi.yaml")));
        var paths = (Map<String, Object>) api.get("paths");
        var endpoint = (Map<String, Object>) paths.get("/api/v1/status");
        var operation = (Map<String, Object>) endpoint.get("get");
        var responses = (Map<String, Object>) operation.get("responses");
        var definition = (Map<String, Object>) responses.get(Integer.toString(expectedCode));
        var content = (Map<String, Object>) definition.get("content");
        var json = (Map<String, Object>) content.get("application/json");
        var schema = (Map<String, Object>) json.get("schema");
        var properties = (Map<String, Object>) schema.get("properties");
        var status = (Map<String, Object>) properties.get("status");
        assertThat(response.statusCode()).isEqualTo(expectedCode);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(response.body()).isEqualTo("{\"status\":\"" + status.get("const") + "\"}");
    }
}

package br.com.concursosimulator.platform;

import br.com.concursosimulator.identity.application.port.AdministrativePasswords;
import br.com.concursosimulator.identity.web.UserPrincipal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Matriz de caracterização das rotas HTTP (backend-http-boundaries, grupo 1).
 *
 * <p>Registra, para cada rota e variante, o resultado observado pela cadeia real de segurança:
 * status, cache, tipo, corpo de erro, redirecionamento, cookie de sessão e CORS. O arquivo
 * {@code src/test/resources/http/route-matrix.tsv} foi gerado na base da épica e só pode mudar
 * quando a spec mudar deliberadamente o contrato. Para regenerar: {@code ROUTE_MATRIX_UPDATE=true}.
 *
 * <p>Limites: corpos de sucesso não são registrados (contêm tokens/ids); requisições sem corpo
 * caracterizam acesso e rejeição, não a validação completa de cada endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Testcontainers
class HttpRouteMatrixTests {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");
    static final Path SNAPSHOT = Path.of("src/test/resources/http/route-matrix.tsv");
    static final String PASSWORD = "synthetic-route-matrix-password";
    static final String ALLOWED_ORIGIN = "http://localhost:5173";
    static final String UNKNOWN_ORIGIN = "https://unknown.example";
    static final String ID = "00000000-0000-4000-8000-000000000001";
    static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("app.question-import.destination-id", () -> "isolated-test");
        r.add("app.question-import.confirmation-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    /** Rotas mapeadas, rotas do framework e variantes inválidas; método e caminho exatos. */
    static final List<String[]> ROUTES = List.of(
            r("GET", "/api/v1/status"),
            r("GET", "/api/v1/csrf"),
            r("GET", "/api/v1/auth/config"),
            r("POST", "/api/v1/auth/logout"),
            r("GET", "/api/v1/me"),
            r("GET", "/oauth2/authorization/google"),
            r("GET", "/login/oauth2/code/google"),
            r("POST", "/oauth2/authorization/google"),
            r("GET", "/api/v1/question-bank/catalog"),
            r("GET", "/api/v1/question-bank/questions"),
            r("GET", "/api/v1/question-bank/questions/" + ID),
            r("POST", "/api/v1/attempts"),
            r("GET", "/api/v1/attempts/" + ID),
            r("GET", "/api/v1/attempts/" + ID + "/result"),
            r("POST", "/api/v1/attempts/" + ID + "/finish"),
            r("PUT", "/api/v1/attempts/" + ID + "/answers/" + ID),
            r("GET", "/api/v1/attempts/" + ID + "/subject-review"),
            r("GET", "/api/v1/me/attempts"),
            r("POST", "/api/v1/lesson-practices"),
            r("GET", "/api/v1/lesson-practices/availability"),
            r("GET", "/api/v1/lesson-practices/" + ID),
            r("GET", "/api/v1/lesson-practices/" + ID + "/result"),
            r("POST", "/api/v1/lesson-practices/" + ID + "/finish"),
            r("PUT", "/api/v1/lesson-practices/" + ID + "/answers/" + ID),
            r("GET", "/api/v1/me/lesson-practices"),
            r("GET", "/api/v1/me/subject-review"),
            r("POST", "/api/v1/admin/auth/login"),
            r("GET", "/api/v1/admin/account"),
            r("POST", "/api/v1/admin/account/password"),
            r("POST", "/api/v1/admin/account/google-link"),
            r("POST", "/api/v1/admin/question-imports/preview"),
            r("POST", "/api/v1/admin/question-imports/apply"),
            r("GET", "/api/v1/admin/questions"),
            r("GET", "/api/v1/admin/questions/" + ID),
            r("POST", "/api/v1/admin/questions/lookup"),
            r("POST", "/api/v1/admin/questions/export"),
            r("POST", "/api/v1/admin/editorial-decisions/preview"),
            r("POST", "/api/v1/admin/editorial-decisions/apply"),
            // Método errado.
            r("DELETE", "/api/v1/me"),
            r("POST", "/api/v1/question-bank/catalog"),
            r("DELETE", "/api/v1/attempts/" + ID),
            r("PATCH", "/api/v1/lesson-practices/" + ID),
            r("POST", "/api/v1/me/subject-review"),
            r("PUT", "/api/v1/admin/account"),
            r("GET", "/api/v1/admin/question-imports/preview"),
            r("DELETE", "/api/v1/admin/questions/" + ID),
            r("POST", "/api/v1/status"),
            // Barra final, segmento vazio e rotas vizinhas inexistentes.
            r("GET", "/api/v1/me/"),
            r("GET", "/api/v1/status/"),
            r("GET", "/api/v1/question-bank/catalog/"),
            r("POST", "/api/v1/attempts/"),
            r("GET", "/api/v1/attempts//result"),
            r("GET", "/api/v1/attempts/" + ID + "/other"),
            r("GET", "/api/v1/lesson-practices/availability/extra"),
            r("GET", "/api/v1/me/other"),
            r("GET", "/api/v1/question-bank"),
            r("GET", "/api/v1/question-bank/other"),
            r("GET", "/api/v1/admin/other"),
            r("GET", "/api/v1/admin/questions/not-a-uuid"),
            r("GET", "/api/v1/unknown"),
            r("GET", "/api/v2/me"));

    enum Actor { VISITOR, VISITOR_SESSION, ACCOUNT, ADMIN, ADMIN_PENDING }
    record Credentials(String cookie, String token) {}

    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdministrativePasswords passwords;
    @Autowired JdbcIndexedSessionRepository sessions;
    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @Test
    void routeMatrixMatchesCharacterization() throws Exception {
        var admin = UUID.randomUUID();
        jdbc.update("insert into app_user(id) values (?)", admin);
        jdbc.update("update admin_registration set owner_id=?,password_hash=?,must_change=false,enabled=true,"
                + "auth_version=auth_version+1,failed_attempts=0,failure_window=null,blocked_until=null",
                admin, passwords.encode(PASSWORD));
        var rows = new ArrayList<String>();
        Credentials adminSession = null;
        for (var actor : Actor.values()) {
            Credentials credentials = switch (actor) {
                case VISITOR -> null;
                case VISITOR_SESSION -> visitorSession();
                case ACCOUNT -> accountSession();
                case ADMIN -> adminSession = adminLogin();
                case ADMIN_PENDING -> {
                    // Mesma sessão administrativa; a troca de senha pendente é lida do estado atual.
                    jdbc.update("update admin_registration set must_change=true");
                    yield adminSession;
                }
            };
            for (var route : ROUTES) {
                boolean safe = route[0].equals("GET");
                for (boolean csrf : safe || credentials == null ? List.of(false) : List.of(false, true)) {
                    var used = credentials;
                    if (csrf && route[1].equals("/api/v1/auth/logout")) {
                        if (actor == Actor.ADMIN || actor == Actor.ADMIN_PENDING) continue; // logout encerraria a sessão compartilhada
                        used = actor == Actor.VISITOR_SESSION ? visitorSession() : accountSession();
                    }
                    var response = send(route[0], route[1], used == null ? null : used.cookie(), csrf ? used.token() : null, null, Map.of());
                    rows.add(key(route, actor, csrf, "-") + "\t" + describe(response));
                }
            }
        }
        for (var route : ROUTES) {
            for (var origin : List.of(ALLOWED_ORIGIN, UNKNOWN_ORIGIN)) {
                var response = send(route[0], route[1], null, null, origin, Map.of());
                rows.add(key(route, Actor.VISITOR, false, origin) + "\t" + describe(response) + "\t" + cors(response));
            }
            var preflight = send("OPTIONS", route[1], null, null, ALLOWED_ORIGIN, Map.of(
                    "Access-Control-Request-Method", route[0],
                    "Access-Control-Request-Headers", "content-type,x-csrf-token"));
            rows.add("OPTIONS " + route[0] + "\t" + route[1] + "\tPREFLIGHT\t-\t" + ALLOWED_ORIGIN + "\t"
                    + preflight.statusCode() + "\t" + cors(preflight));
        }
        compare(rows);
    }

    void compare(List<String> rows) throws Exception {
        if ("true".equals(System.getenv("ROUTE_MATRIX_UPDATE"))) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, String.join("\n", rows) + "\n", StandardCharsets.UTF_8);
            return;
        }
        assertThat(SNAPSHOT).as("Gere a matriz com ROUTE_MATRIX_UPDATE=true na base da épica").exists();
        var expected = index(Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8));
        var actual = index(rows);
        var differences = new ArrayList<String>();
        for (var entry : expected.entrySet())
            if (!entry.getValue().equals(actual.get(entry.getKey())))
                differences.add(entry.getKey() + "\n  esperado: " + entry.getValue() + "\n  obtido:   " + actual.get(entry.getKey()));
        for (var key : actual.keySet()) if (!expected.containsKey(key)) differences.add(key + "\n  linha nova: " + actual.get(key));
        if (!differences.isEmpty())
            fail(differences.size() + " diferenças na matriz de rotas:\n" + String.join("\n", differences.subList(0, Math.min(40, differences.size()))));
    }

    static Map<String, String> index(List<String> lines) {
        var result = new LinkedHashMap<String, String>();
        for (var line : lines) {
            if (line.isBlank()) continue;
            var fields = line.split("\t", 6);
            var previous = result.put(String.join("\t", fields[0], fields[1], fields[2], fields[3], fields[4]), fields[5]);
            assertThat(previous).as("linha duplicada na matriz: " + line).isNull();
        }
        return result;
    }

    static String key(String[] route, Actor actor, boolean csrf, String origin) {
        return route[0] + "\t" + route[1] + "\t" + actor + "\t" + (csrf ? "csrf" : "-") + "\t" + origin;
    }

    static String describe(HttpResponse<String> response) {
        var body = response.statusCode() >= 300 && !response.body().isEmpty()
                ? UUID_TEXT.matcher(response.body()).replaceAll("<uuid>") : response.body().isEmpty() ? "<vazio>" : "<corpo>";
        if (body.length() > 160) body = body.substring(0, 160) + "…";
        var cookies = response.headers().allValues("Set-Cookie").stream().filter(c -> c.startsWith("SESSION=")).toList();
        var session = cookies.isEmpty() ? "-" : cookies.stream().anyMatch(c -> c.contains("Max-Age=0")) ? "cleared" : "set";
        var location = response.headers().firstValue("Location").map(l -> l.replaceFirst("^https?://[^/]+", "")).orElse("-");
        return response.statusCode() + "\t" + response.headers().firstValue("Cache-Control").orElse("-") + "\t"
                + response.headers().firstValue("Content-Type").orElse("-") + "\t" + session + "\t" + location + "\t" + body;
    }

    static String cors(HttpResponse<String> response) {
        return "ACAO=" + response.headers().firstValue("Access-Control-Allow-Origin").orElse("-")
                + " ACAC=" + response.headers().firstValue("Access-Control-Allow-Credentials").orElse("-")
                + " ACAM=" + response.headers().firstValue("Access-Control-Allow-Methods").orElse("-")
                + " ACAH=" + response.headers().firstValue("Access-Control-Allow-Headers").orElse("-");
    }

    Credentials visitorSession() throws Exception {
        var response = send("GET", "/api/v1/csrf", null, null, null, Map.of());
        var cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        return new Credentials(cookie, json.readTree(response.body()).get("token").asString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    Credentials accountSession() throws Exception {
        // Fixture de teste: sessão autenticada gravada direto no repositório, como em SessionAccessTests.
        var repository = (SessionRepository<Session>) (SessionRepository) sessions;
        var session = repository.createSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(UUID.randomUUID()), null, List.of())));
        repository.save(session);
        var cookie = "SESSION=" + session.getId();
        return new Credentials(cookie, token(cookie));
    }

    Credentials adminLogin() throws Exception {
        var visitor = visitorSession();
        var login = send("POST", "/api/v1/admin/auth/login", visitor.cookie(), visitor.token(), null,
                Map.of("Content-Type", "application/json"), "{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(204);
        var cookie = login.headers().allValues("Set-Cookie").stream().filter(c -> c.startsWith("SESSION="))
                .findFirst().map(c -> c.split(";", 2)[0]).orElse(visitor.cookie());
        return new Credentials(cookie, token(cookie));
    }

    String token(String cookie) throws Exception {
        return json.readTree(send("GET", "/api/v1/csrf", cookie, null, null, Map.of()).body()).get("token").asString();
    }

    HttpResponse<String> send(String method, String path, String cookie, String csrf, String origin, Map<String, String> headers) throws Exception {
        return send(method, path, cookie, csrf, origin, headers, null);
    }

    HttpResponse<String> send(String method, String path, String cookie, String csrf, String origin,
                              Map<String, String> headers, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(20))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) request.header("Cookie", cookie);
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        if (origin != null) request.header("Origin", origin);
        headers.forEach(request::header);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String[] r(String method, String path) { return new String[]{method, path}; }
}

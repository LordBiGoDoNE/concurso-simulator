package br.com.concursosimulator.identity;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@Import(OidcTestRegistration.class)
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class GoogleOidcTests {
    static final OidcFixture OIDC = new OidcFixture();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void configuration(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.google.enabled", () -> true);
        registry.add("fixture.issuer", OIDC::issuer);
        registry.add("app.google.callback-url", () -> "{baseUrl}/login/oauth2/code/google");
        registry.add("app.google.frontend-url", () -> "http://127.0.0.1:5173");
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @AfterAll static void closeProvider() { OIDC.close(); }

    @Test
    void fullLoginPkceSessionRotationAndMinimalPersistence(CapturedOutput output) throws Exception {
        OIDC.mode = "valid"; OIDC.subject = "returning-subject";
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        get(client, base() + "/api/v1/csrf");
        String oldId = cookies.getCookieStore().getCookies().getFirst().getValue();
        var callback = callback(client, base() + "/oauth2/authorization/google?returnTo=https://evil.example");
        var result = get(client, callback);
        assertThat(result.statusCode()).isEqualTo(302);
        assertThat(result.headers().firstValue("Location")).contains("http://127.0.0.1:5173");
        assertThat(OIDC.pkceVerified).isTrue();
        String newId = cookies.getCookieStore().getCookies().getFirst().getValue();
        assertThat(newId).isNotEqualTo(oldId);
        var me = get(client, base() + "/api/v1/me");
        HttpContract.json("/api/v1/me", 200, me);
        assertThat(get(HttpClient.newHttpClient(), base() + "/api/v1/me").statusCode()).isEqualTo(401);
        var fixed = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base() + "/api/v1/me"))
                .header("Cookie", "SESSION=" + oldId).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(fixed.statusCode()).isEqualTo(401);
        assertThat(get(client, callback).headers().firstValue("Location")).contains("http://127.0.0.1:5173?auth=failed");
        assertThat(get(client, base() + "/api/v1/me").statusCode()).isEqualTo(401);
        var second = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        get(second, callback(second, base() + "/oauth2/authorization/google"));
        assertThat(get(second, base() + "/api/v1/me").body()).isEqualTo(me.body());
        for (byte[] bytes : jdbc.queryForList("SELECT attribute_bytes FROM spring_session_attributes", byte[].class)) {
            String serialized = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(serialized).doesNotContain("fixture-access-", "Google fixture profile", "OidcIdToken", OIDC.lastIdToken);
        }
        assertThat(output.getAll()).doesNotContain("fixture-access-", OIDC.lastIdToken, "returning-subject");
    }

    @ParameterizedTest
    @ValueSource(strings = {"nonce", "issuer", "audience", "signature", "expired", "cancel", "state"})
    void invalidResponsesDoNotCreateAccountsOrSessions(String mode, CapturedOutput output) throws Exception {
        OIDC.mode = mode; OIDC.subject = "invalid-" + mode;
        int users = jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class);
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String callback = callback(client, base() + "/oauth2/authorization/google");
        if (mode.equals("state")) callback = callback.replaceAll("state=[^&]+", "state=wrong-state");
        var result = get(client, callback);
        assertThat(result.headers().firstValue("Location")).contains("http://127.0.0.1:5173?auth=failed");
        assertThat(get(client, base() + "/api/v1/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(users);
        assertThat(result.body()).doesNotContain("fixture-access-", "id_token", "invalid-");
        assertThat(output.getAll()).doesNotContain("fixture-access-", "invalid-" + mode);
        if (OIDC.lastIdToken != null) assertThat(output.getAll()).doesNotContain(OIDC.lastIdToken);
    }

    @Test
    void publicConfigurationExposesNoSecrets() throws Exception {
        var config = get(HttpClient.newHttpClient(), base() + "/api/v1/auth/config");
        HttpContract.json("/api/v1/auth/config", 200, config);
        assertThat(config.body()).isEqualTo("{\"googleEnabled\":true}");
    }

    String callback(HttpClient client, String entry) throws Exception {
        String authorize = get(client, entry).headers().firstValue("Location").orElseThrow();
        assertThat(OidcFixture.parameters(URI.create(authorize).getRawQuery()).get("code_challenge_method")).isEqualTo("S256");
        return get(client, authorize).headers().firstValue("Location").orElseThrow();
    }
    String base() { return "http://127.0.0.1:" + port; }
    HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}

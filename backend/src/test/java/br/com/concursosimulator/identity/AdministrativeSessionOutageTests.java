package br.com.concursosimulator.identity;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Testcontainers
class AdministrativeSessionOutageTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);}
    @Value("${local.server.port}")int port;
    @Test void sessionOutageBeforeSecurityPreservesAdministrative503NoStoreAndKnownCors()throws Exception {
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String base="http://127.0.0.1:"+port;
        var csrf=c.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var token=JsonMapper.builder().build().readTree(csrf.body()).get("token").asString();PG.stop();
        for(var path:new String[]{"/api/v1/admin/account","/api/v1/admin/auth/login"}) {
            var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(java.time.Duration.ofSeconds(10)).header("Origin","http://localhost:5173");
            if(path.endsWith("/login"))builder.header("X-CSRF-TOKEN",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"admin\",\"password\":\"PRIVATE_PASSWORD_SENTINEL\"}"));
            else builder.GET();
            var response=c.send(builder.build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(503);assertThat(response.body()).contains("ADMIN_UNAVAILABLE","correlationId").doesNotContain("PRIVATE_PASSWORD_SENTINEL",token,"jdbc:");
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
            assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        }
        assertThat(c.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/status")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(503);
        assertThat(c.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/question-bank/catalog")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
    }
}

package br.com.concursosimulator.questionbank;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.hikari.connection-timeout=1000"})
@ActiveProfiles("local") @Testcontainers
class ImportSessionOutageTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    }
    @Value("${local.server.port}")int port;
    @Test void sessionDatabaseOutageBeforeMvcReturnsSanitized503AndKnownCors()throws Exception{
        var http=HttpClient.newHttpClient();var base="http://127.0.0.1:"+port;
        var csrf=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var cookie=csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        PG.stop();
        var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/admin/question-imports/preview")).timeout(Duration.ofSeconds(15))
                .header("Cookie",cookie).header("Origin","http://localhost:5173").header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"PRIVATE_SENTINEL\":true}")).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(503);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(JsonMapper.builder().build().readTree(response.body()).path("error").asString()).isEqualTo("IMPORT_UNAVAILABLE");
        assertThat(response.body()).doesNotContain("PRIVATE_SENTINEL","jdbc:","password","Exception");
    }
}

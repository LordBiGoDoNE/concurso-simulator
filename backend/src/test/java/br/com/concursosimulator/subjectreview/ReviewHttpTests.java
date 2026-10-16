package br.com.concursosimulator.subjectreview;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.QuestionBrowserFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("local") @Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReviewHttpTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    }
    @Autowired ConfigurableApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Value("${local.server.port}") int port;
    final HttpClient client=HttpClient.newHttpClient();final JsonMapper json=JsonMapper.builder().build();
    final String panel="/api/v1/me/subject-review",role="?roleId=assistente-administrativo";
    record Browser(String cookie,String token) {}
    @BeforeEach void seed() { if(jdbc.queryForObject("select count(*) from qb_question",Integer.class)==0)QuestionBrowserFixtures.seed(context); }

    @Test @Order(1) void accountPanelAndIndividualHaveClosedSchemasAndDoNotLoadHistoryForVisitors() throws Exception {
        var account=account(UUID.randomUUID());var empty=send("GET",panel+role,account,null);assertThat(empty.statusCode()).isEqualTo(200);schema(empty.body(),"SubjectReviewOverview");
        var data=json.readTree(empty.body());assertThat(data.get("subjects").size()).isEqualTo(51);assertThat(data.get("window").get("attemptCount").asInt()).isZero();
        assertThat(data.get("subjects").get(0).get("percentage").isNull()).isTrue();
        assertThat(send("GET",panel+"?roleId=secretario-de-escola",account,null).statusCode()).isEqualTo(200);
        for(var actor:List.of(account,browser(null))) {
            var v=create(actor);var path="/api/v1/attempts/"+v.get("id").asString();
            assertThat(send("GET",path+"/subject-review",actor,null).statusCode()).isEqualTo(409);
            assertThat(send("POST",path+"/finish",actor,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}").statusCode()).isEqualTo(200);
            var one=send("GET",path+"/subject-review",actor,null);assertThat(one.statusCode()).isEqualTo(200);schema(one.body(),"SubjectReviewIndividual");
            var i=json.readTree(one.body());assertThat(i.get("attemptId").asString()).isEqualTo(v.get("id").asString());
            assertThat(i.get("subjects").get(0).get("blank").asInt()).isEqualTo(3);assertThat(i.has("window")).isFalse();
            assertThat(send("GET",path+"/subject-review",browser(null),null).statusCode()).isEqualTo(404);
        }
        var summary=json.readTree(send("GET",panel+role,account,null).body());assertThat(summary.get("window").get("attemptCount").asInt()).isEqualTo(1);
    }
    @Test @Order(2) void inputsAreStrictAndRouteSecurityRejectsBeforeTheController() throws Exception {
        var a=account(UUID.randomUUID());
        for(var query:List.of("","?roleId=","?roleId=outside",role+"&roleId=secretario-de-escola",role+"&limit=1",role+"&owner="+UUID.randomUUID(),role+"&useParameterizedQuestions=true")) {
            var r=send("GET",panel+query,a,null);assertThat(r.statusCode()).as(query).isEqualTo(400);assertThat(r.body()).isEqualTo("{\"error\":\"invalid_request\"}");
        }
        for(var id:List.of("invalid","1-1-1-1-1"))assertThat(send("GET","/api/v1/attempts/"+id+"/subject-review",a,null).statusCode()).isEqualTo(400);
        var path="/api/v1/attempts/"+UUID.randomUUID()+"/subject-review";
        for(var q:List.of("roleId=assistente-administrativo","page=0","x=1&x=2"))assertThat(send("GET",path+"?"+q,a,null).statusCode()).isEqualTo(400);
        for(Authentication auth:new Authentication[]{null,UsernamePasswordAuthenticationToken.authenticated("invalid",null,List.of()),
                UsernamePasswordAuthenticationToken.unauthenticated(new UserPrincipal(UUID.randomUUID()),null),
                new AnonymousAuthenticationToken("fixture",new UserPrincipal(UUID.randomUUID()),List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")))}) {
            var b=session(auth);var r=send("GET",panel,b,null);assertThat(r.statusCode()).isEqualTo(401);assertThat(r.body()).isEqualTo("{\"error\":\"unauthenticated\"}");
            if(auth!=null)assertThat(send("GET",path,b,null).statusCode()).isEqualTo(401);
        }
        assertThat(send("GET",panel+role,browser(null),null).statusCode()).isEqualTo(401);
        var noCsrf=new Browser(a.cookie(),null);assertThat(send("POST","/api/v1/attempts",noCsrf,createBody()).statusCode()).isEqualTo(403);
    }
    @Test @Order(3) void sameAccountAnotherSessionSeesItsEvidenceButOtherAccountAndExpiredVisitorDoNot() throws Exception {
        var id=UUID.randomUUID();var a=account(id);var view=create(a);var path="/api/v1/attempts/"+view.get("id").asString();
        send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        var fresh=account(id);assertThat(send("GET",path+"/subject-review",fresh,null).body()).isEqualTo(send("GET",path+"/subject-review",a,null).body());
        var other=account(UUID.randomUUID());var hidden=send("GET",path+"/subject-review",other,null);var absent=send("GET","/api/v1/attempts/"+UUID.randomUUID()+"/subject-review",other,null);
        assertThat(hidden.statusCode()).isEqualTo(404);assertThat(hidden.body()).isEqualTo(absent.body());
        assertThat(json.readTree(send("GET",panel+role,other,null).body()).get("window").get("attemptCount").asInt()).isZero();
        var guest=browser(null);send("GET","/api/v1/attempts/"+UUID.randomUUID(),guest,null);var session=repository().findById(guest.cookie().substring(8));
        session.setLastAccessedTime(Instant.now().minusSeconds(3600));repository().save(session);
        assertThat(send("GET",path+"/subject-review",guest,null).statusCode()).isEqualTo(404);
    }
    String createBody() { return "{\"commandId\":\""+UUID.randomUUID()+"\",\"roleId\":\"assistente-administrativo\",\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":3,\"topicIds\":[]}]}"; }
    JsonNode create(Browser b) throws Exception { var r=send("POST","/api/v1/attempts",b,createBody());assertThat(r.statusCode()).isEqualTo(201);return json.readTree(r.body()); }
    Browser account(UUID id) throws Exception {
        jdbc.update("insert into app_user(id,created_at) values (?,now()) on conflict do nothing",id);
        return browser(session(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(id),null,List.of())).cookie());
    }
    Browser session(Authentication a) { var s=repository().createSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(a));repository().save(s);return new Browser("SESSION="+s.getId(),null); }
    Browser browser(String cookie) throws Exception { var r=send("GET","/api/v1/csrf",new Browser(cookie,null),null);return new Browser(cookie==null?r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0]:cookie,json.readTree(r.body()).get("token").asString()); }
    @SuppressWarnings({"unchecked","rawtypes"}) SessionRepository<Session> repository() { return (SessionRepository)sessions; }
    HttpResponse<String> send(String method,String path,Browser b,String body) throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if(b!=null&&b.cookie()!=null)r.header("Cookie",b.cookie());if(b!=null&&b.token()!=null)r.header("X-CSRF-TOKEN",b.token());if(body!=null)r.header("Content-Type","application/json");
        var response=client.send(r.build(),HttpResponse.BodyHandlers.ofString());
        if(path.contains("subject-review")){assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");assertThat(response.headers().firstValue("Location")).isEmpty();}
        return response;
    }
    @SuppressWarnings("unchecked") void schema(String body,String name) throws Exception {
        Map<String,Object> api=new org.yaml.snakeyaml.Yaml().load(java.nio.file.Files.readString(java.nio.file.Path.of("openapi.yaml")));
        var schemas=(Map<String,Object>)((Map<String,Object>)api.get("components")).get("schemas");check(json.readTree(body),(Map<String,Object>)schemas.get(name),schemas);
    }
    @SuppressWarnings("unchecked") void check(JsonNode value,Map<String,Object> schema,Map<String,Object> schemas) {
        if(schema.containsKey("$ref")){check(value,(Map<String,Object>)schemas.get(schema.get("$ref").toString().replace("#/components/schemas/","")),schemas);return;}
        var type=schema.get("type");if(type instanceof List<?> types){if(value.isNull()){assertThat(types.contains("null")).isTrue();return;}type=types.stream().filter(t->!t.equals("null")).findFirst().orElseThrow();}
        switch(type.toString()) {
            case "object" -> {assertThat(value.isObject()).isTrue();assertThat(schema.get("additionalProperties")).isEqualTo(false);var props=(Map<String,Object>)schema.get("properties");assertThat(value.propertyNames()).containsExactlyInAnyOrderElementsOf(props.keySet());for(var key:props.keySet())check(value.get(key),(Map<String,Object>)props.get(key),schemas);}
            case "array" -> {assertThat(value.isArray()).isTrue();for(var item:value)check(item,(Map<String,Object>)schema.get("items"),schemas);}
            case "string" -> assertThat(value.isString()).isTrue();case "integer" -> assertThat(value.isIntegralNumber()).isTrue();case "number" -> assertThat(value.isNumber()).isTrue();case "boolean" -> assertThat(value.isBoolean()).isTrue();default -> throw new AssertionError("Tipo não coberto");
        }
    }
}

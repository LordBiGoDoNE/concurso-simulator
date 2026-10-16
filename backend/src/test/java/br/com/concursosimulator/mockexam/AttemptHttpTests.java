package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.QuestionBrowserFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("local") @Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttemptHttpTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    }
    @Autowired ConfigurableApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcIndexedSessionRepository sessions;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    br.com.concursosimulator.parameterization.infrastructure.persistence.JpaEligibleTemplates templates;
    @Value("${local.server.port}") int port;
    final HttpClient client=HttpClient.newHttpClient(); final JsonMapper json=JsonMapper.builder().build();
    record Browser(String cookie,String token) {}
    @BeforeEach void seed() { if (jdbc.queryForObject("select count(*) from qb_question",Integer.class)==0) QuestionBrowserFixtures.seed(context); }
    @Test @Order(1) void visitorCreatesRetriesReadsAnswersAndClearsWithoutAnyCorrectiveFields() throws Exception {
        var browser=browser(null);var command=UUID.randomUUID();var request=createBody(command,2);
        var first=send("POST","/api/v1/attempts",browser,request); assertThat(first.statusCode()).isEqualTo(201);
        assertSchema(first.body(),"AttemptView");assertSafe(first.body());var view=json.readTree(first.body());var id=view.get("id").asString();
        var retry=send("POST","/api/v1/attempts",browser,request);assertThat(retry.statusCode()).isEqualTo(200);assertThat(retry.body()).isEqualTo(first.body());
        var read=send("GET","/api/v1/attempts/"+id,browser,null);assertThat(read.body()).isEqualTo(first.body());
        var item=view.get("items").get(0);var op=UUID.randomUUID();var alt=item.get("alternatives").get(1).get("id").asString();
        var answer="{\"commandId\":\""+op+"\",\"expectedVersion\":0,\"alternativeId\":\""+alt+"\"}";
        var url="/api/v1/attempts/"+id+"/answers/"+item.get("id").asString();
        assertThat(send("PUT",url,browser,"{\"commandId\":\""+op+"\",\"expectedVersion\":0}").statusCode()).isEqualTo(400);
        assertThat(send("PUT",url,browser,"{\"commandId\":\""+op+"\",\"expectedVersion\":\"0\",\"alternativeId\":null}").statusCode()).isEqualTo(400);
        var saved=send("PUT",url,browser,answer);assertThat(saved.statusCode()).isEqualTo(200);assertSafe(saved.body());assertSchema(saved.body(),"AttemptView");
        assertThat(json.readTree(saved.body()).get("items").get(0).get("selectedAlternativeId").asString()).isEqualTo(alt);
        assertThat(send("PUT",url,browser,answer).body()).isEqualTo(saved.body());
        var cleared=send("PUT",url,browser,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":1,\"alternativeId\":null}");
        assertThat(cleared.statusCode()).isEqualTo(200);assertThat(json.readTree(cleared.body()).get("items").get(0).get("selectedAlternativeId").isNull()).isTrue();
        assertThat(send("PUT",url,browser,answer.replace(alt,UUID.randomUUID().toString())).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("select count(*) from app_user",Integer.class)).isZero();
    }
    @Test @Order(2) void invalidInputsCsrfAndInsufficientPoolNeverCreatePartialAttempts() throws Exception {
        var b=browser(null);int before=jdbc.queryForObject("select count(*) from me_attempt",Integer.class);
        var valid=createBody(UUID.randomUUID(),1);
        for (var body:List.of(valid.replace("\"quantity\":1","\"quantity\":1.5"),valid.replace("\"quantity\":1","\"quantity\":\"1\""),valid.replace("\"quantity\":1","\"quantity\":null"),
                valid.replace("\"quantity\":1","\"quantity\":0"),valid.replace("assistente-administrativo","outside"),
                valid.replace("\"portugues\"","\"outside\""),valid.replace("\"topicIds\":[]","\"topicIds\":[\"M01\"]"),
                valid.substring(0,valid.length()-1)+",\"owner\":\""+UUID.randomUUID()+"\"}",valid.substring(0,valid.length()-1)+",\"correctAlternativeId\":\""+UUID.randomUUID()+"\"}")) {
            var r=send("POST","/api/v1/attempts",b,body);assertThat(r.statusCode()).as(body).isEqualTo(400);assertThat(r.body()).isEqualTo("{\"error\":\"invalid_request\"}");
        }
        var noCsrf=new Browser(b.cookie(),null);assertThat(send("POST","/api/v1/attempts",noCsrf,valid).statusCode()).isEqualTo(403);
        var missing=send("POST","/api/v1/attempts",b,createBody(UUID.randomUUID(),100));assertThat(missing.statusCode()).isEqualTo(409);
        assertThat(missing.body()).contains("insufficient_pool","available");
        assertThat(jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(before);
    }
    @Test @Order(3) void wrongOwnerAndMissingResourceHaveIndistinguishableJsonAndSchemaControlDetectsLeak() throws Exception {
        var owner=browser(null);var other=browser(null);var created=send("POST","/api/v1/attempts",owner,createBody(UUID.randomUUID(),1));
        var view=json.readTree(created.body());var path="/api/v1/attempts/"+view.get("id").asString();
        var forbidden=send("GET",path,other,null);var missing=send("GET","/api/v1/attempts/"+UUID.randomUUID(),other,null);
        assertThat(forbidden.statusCode()).isEqualTo(404);assertThat(forbidden.body()).isEqualTo(missing.body());
        assertThat(send("GET",path+"?owner="+UUID.randomUUID(),owner,null).statusCode()).isEqualTo(400);
        var item=view.get("items").get(0).get("id").asString();
        assertThat(send("PUT",path+"/answers/"+item,other,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":null}").statusCode()).isEqualTo(404);
        var mutant=(tools.jackson.databind.node.ObjectNode)view;mutant.put("correctAlternativeId",UUID.randomUUID().toString());
        assertThatThrownBy(() -> assertSchema(mutant.toString(),"AttemptView")).isInstanceOf(AssertionError.class);
    }
    @Test @Order(4) void finishUsesCapturedCorrectionAllowsBlanksAndReplaysWithoutAnotherTransition() throws Exception {
        var b=browser(null);var outsider=browser(null);var initial=send("POST","/api/v1/attempts",b,createBody(UUID.randomUUID(),5));
        var v=json.readTree(initial.body());var path="/api/v1/attempts/"+v.get("id").asString();
        assertThat(send("GET",path+"/result",b,null).statusCode()).isEqualTo(409);
        assertThat(send("GET",path+"/result",outsider,null).statusCode()).isEqualTo(404);
        for (int i=0;i<4;i++) {
            var item=v.get("items").get(i);var option=i<3?br.com.concursosimulator.questionbank.QuestionFixtures.FIRST:br.com.concursosimulator.questionbank.QuestionFixtures.SECOND;
            assertThat(send("PUT",path+"/answers/"+item.get("id").asString(),b,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":"+i+",\"alternativeId\":\""+option+"\"}").statusCode()).isEqualTo(200);
        }
        var op=UUID.randomUUID();var command="{\"commandId\":\""+op+"\",\"expectedVersion\":4}";
        assertThat(send("POST",path+"/finish",outsider,command).statusCode()).isEqualTo(404);
        assertThat(send("POST",path+"/finish",new Browser(b.cookie(),null),command).statusCode()).isEqualTo(403);
        assertThat(send("POST",path+"/finish",b,command.replace("4}","3}")).statusCode()).isEqualTo(409);
        var finish=send("POST",path+"/finish",b,command);assertThat(finish.statusCode()).isEqualTo(200);assertSchema(finish.body(),"AttemptFinished");
        var result=json.readTree(finish.body()).get("result");
        assertThat(result.get("correct").asInt()).isEqualTo(3);assertThat(result.get("points").asInt()).isEqualTo(3);
        assertThat(result.get("wrong").asInt()).isEqualTo(1);assertThat(result.get("blank").asInt()).isEqualTo(1);assertThat(result.get("percentage").asDouble()).isEqualTo(60);
        assertThat(result.get("items").size()).isEqualTo(5);assertThat(finish.body()).contains("PRIVATE_BROWSER_RESOLUTION_SENTINEL");
        assertThat(send("POST",path+"/finish",b,command).body()).isEqualTo(finish.body());
        assertThat(json.readTree(send("GET",path+"/result",b,null).body())).isEqualTo(result);
        assertSafe(send("GET",path,b,null).body());
        assertThat(send("PUT",path+"/answers/"+v.get("items").get(0).get("id").asString(),b,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":5,\"alternativeId\":null}").statusCode()).isEqualTo(409);
    }
    @Test @Order(5) void historyIsPaginatedPrivateAndResumableWithAnotherSessionOfTheSameAccount() throws Exception {
        var owner=UUID.randomUUID();var a=account(owner);var other=account(UUID.randomUUID());
        var first=json.readTree(send("POST","/api/v1/attempts",a,createBody(UUID.randomUUID(),1)).body());
        var second=json.readTree(send("POST","/api/v1/attempts",a,createBody(UUID.randomUUID(),2)).body());
        var path="/api/v1/attempts/"+first.get("id").asString();
        send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        var fresh=account(owner);var page=send("GET","/api/v1/me/attempts?page=0&size=1",fresh,null);assertThat(page.statusCode()).isEqualTo(200);assertSchema(page.body(),"AttemptHistory");
        assertThat(json.readTree(page.body()).get("items").get(0).get("id").asString()).isEqualTo(second.get("id").asString());
        assertThat(json.readTree(page.body()).get("items").get(0).get("percentage").isNull()).isTrue();
        var next=json.readTree(send("GET","/api/v1/me/attempts?page=1&size=1",fresh,null).body());assertThat(next.get("items").get(0).get("id").asString()).isEqualTo(first.get("id").asString());
        assertThat(next.get("items").get(0).get("percentage").asDouble()).isEqualTo(0);
        assertThat(json.readTree(send("GET","/api/v1/me/attempts",other,null).body()).get("items").size()).isZero();
        assertThat(send("GET",path,other,null).statusCode()).isEqualTo(404);
        assertThat(send("GET","/api/v1/attempts/"+second.get("id").asString(),fresh,null).body()).isEqualTo(second.toString());
        assertThat(send("GET","/api/v1/me/attempts",browser(null),null).statusCode()).isEqualTo(401);
        for(var q:List.of("page=-1","size=101","finished=invalid","owner="+owner,"page=0&page=1"))assertThat(send("GET","/api/v1/me/attempts?"+q,fresh,null).statusCode()).isEqualTo(400);
        assertThat(json.readTree(send("GET","/api/v1/me/attempts?finished=true",fresh,null).body()).get("items").size()).isEqualTo(1);
        var logout=send("POST","/api/v1/auth/logout",a,null);assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(send("GET","/api/v1/me/attempts",a,null).statusCode()).isEqualTo(401);
        assertThat(send("GET",path,fresh,null).statusCode()).isEqualTo(200);
    }
    @Test @Order(6) void numericOptionIsStrictAndLegacyFalseReplaysTheSameCommand() throws Exception {
        var b=browser(null);var request=createBody(UUID.randomUUID(),1);
        var old=send("POST","/api/v1/attempts",b,request);assertThat(old.statusCode()).isEqualTo(201);
        assertThat(send("POST","/api/v1/attempts",b,option(request,"false")).body()).isEqualTo(old.body());
        assertThat(send("POST","/api/v1/attempts",b,option(request,"true")).statusCode()).isEqualTo(409);
        var before=jdbc.queryForObject("select count(*) from me_attempt",Integer.class);
        for(var value:List.of("null","\"true\"","1","0","[]","{}")) {
            var r=send("POST","/api/v1/attempts",b,option(createBody(UUID.randomUUID(),1),value));
            assertThat(r.statusCode()).as(value).isEqualTo(400);assertThat(r.body()).isEqualTo("{\"error\":\"invalid_request\"}");
        }
        assertThat(send("POST","/api/v1/attempts",new Browser(b.cookie(),null),option(request,"true")).statusCode()).isEqualTo(403);
        assertThat(send("POST","/api/v1/attempts",b,option(request,"true").replace("\"useParameterizedQuestions\":true","\"useParameterizedQuestions\":true,\"parameters\":{}")).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(before);
    }
    @Test @Order(7) void generatedAndFixedItemsStayPrivateAndAccountCanResumeAfterWithdrawal() throws Exception {
        var versions=br.com.concursosimulator.parameterization.ParameterizedBrowserFixtures.seed(context);
        var owner=UUID.randomUUID();var account=account(owner);var visitor=browser(null);
        var store=context.getBean(br.com.concursosimulator.mockexam.application.AttemptStore.class);
        String accountId=null;
        for(var actor:List.of(visitor,account)) {
            var request=option(createBody(UUID.randomUUID(),4).replace("portugues","matematica-raciocinio"),"true");
            var created=send("POST","/api/v1/attempts",actor,request);assertThat(created.statusCode()).isEqualTo(201);assertSchema(created.body(),"AttemptView");
            assertSafe(created.body());assertThat(created.body()).doesNotContain("templateId","parameters","manifestDigest","outputDigest","generatorVersion","\"source\"");
            assertThat(send("POST","/api/v1/attempts",actor,request).body()).isEqualTo(created.body());
            var id=UUID.fromString(json.readTree(created.body()).get("id").asString());
            var captured=store.transaction(s->s.find(id).orElseThrow());
            assertThat(captured.items().stream().filter(i->!i.parameters().isEmpty())).hasSize(3);
            assertThat(captured.items().stream().map(i->i.question().questionId()).distinct()).hasSize(4);
            var item=captured.items().getFirst();var path="/api/v1/attempts/"+id;
            assertThat(send("PUT",path+"/answers/"+item.id(),actor,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":\""+item.question().content().correctAlternativeId()+"\"}").statusCode()).isEqualTo(200);
            var finished=send("POST",path+"/finish",actor,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":1}");assertThat(finished.statusCode()).isEqualTo(200);assertSchema(finished.body(),"AttemptFinished");
            assertThat(json.readTree(finished.body()).get("result").get("correct").asInt()).isEqualTo(1);
            assertThat(finished.body()).doesNotContain("templateId","parameters","manifestDigest","outputDigest","generatorVersion");
            if(actor==account)accountId=id.toString();
        }
        var uc=new br.com.concursosimulator.parameterization.application.TemplateUseCases(context.getBean(br.com.concursosimulator.parameterization.application.TemplateStore.class));
        for(var v:versions)uc.decide(br.com.concursosimulator.parameterization.ParameterFixtures.decision(v,br.com.concursosimulator.parameterization.domain.TemplateDecision.Action.WITHDRAW),1,2);
        var fresh=account(owner);assertThat(send("GET","/api/v1/attempts/"+accountId+"/result",fresh,null).statusCode()).isEqualTo(200);
        assertThat(send("GET","/api/v1/attempts/"+accountId,browser(null),null).statusCode()).isEqualTo(404);
        assertThat(send("GET","/api/v1/me/attempts",fresh,null).body()).contains(accountId);
    }
    @Test @Order(8) void generationIntegrityFailureIsGenericAndRollsBackTheWholeRequest() throws Exception {
        var b=browser(null);var tables=List.of("me_attempt","me_item","pq_instance","me_command");
        var before=tables.stream().map(t->jdbc.queryForObject("select count(*) from "+t,Integer.class)).toList();
        org.mockito.Mockito.doThrow(new IllegalStateException("PRIVATE_PARAMETERIZED_FAILURE definitionDigest=secret")).when(templates).capture();
        try {
            var r=send("POST","/api/v1/attempts",b,option(createBody(UUID.randomUUID(),1),"true"));
            assertThat(r.statusCode()).isEqualTo(503);assertThat(r.body()).isEqualTo("{\"error\":\"unavailable\"}");
            assertThat(tables.stream().map(t->jdbc.queryForObject("select count(*) from "+t,Integer.class)).toList()).isEqualTo(before);
        } finally { org.mockito.Mockito.reset(templates); }
    }
    String option(String body,String value){return body.substring(0,body.length()-1)+",\"useParameterizedQuestions\":"+value+"}";}
    @Test @Order(90) void incompatibleProjectionReturns503WithoutLeakingOrWritingAnyAttempt() throws Exception {
        var store=context.getBean(br.com.concursosimulator.questionbank.application.port.QuestionStore.class);
        var c=br.com.concursosimulator.questionbank.QuestionFixtures.content("<span data-correct='true'>PRIVATE_MARKUP_SENTINEL</span>");
        var q=br.com.concursosimulator.questionbank.domain.Question.draft(UUID.randomUUID(),new br.com.concursosimulator.questionbank.domain.QuestionOrigin("negative-attempt",UUID.randomUUID().toString()),UUID.randomUUID(),c);
        store.transaction(s -> {s.insert(q);return null;});
        for (var action:List.of(br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.SUBMIT,br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.APPROVE)) {
            store.transaction(s -> {var loaded=s.loadForUpdate(q.id()).orElseThrow(); loaded.decide(new br.com.concursosimulator.questionbank.domain.EditorialDecision(action,q.id(),q.latest().id(),c.fingerprint(),"Fixture",java.time.Instant.now(),Set.of(br.com.concursosimulator.questionbank.domain.EditorialDecision.Check.values()),"Controle negativo sintético"));s.save(loaded);return null;});
        }
        int before=jdbc.queryForObject("select count(*) from me_attempt",Integer.class);
        try {
            var r=send("POST","/api/v1/attempts",browser(null),createBody(UUID.randomUUID(),1));
            assertThat(r.statusCode()).isEqualTo(503);assertThat(r.body()).isEqualTo("{\"error\":\"unavailable\"}");
            assertThat(jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(before);
        } finally { store.transaction(s -> {var loaded=s.loadForUpdate(q.id()).orElseThrow();loaded.decide(new br.com.concursosimulator.questionbank.domain.EditorialDecision(br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.WITHDRAW,q.id(),q.latest().id(),c.fingerprint(),"Fixture",java.time.Instant.now(),Set.of(),"Remover controle negativo"));s.save(loaded);return null;}); }
    }
    @Test @Order(99) void unavailableSessionDatabaseReturns503BeforeMvcAndReadinessStillWorks() throws Exception {
        var b=browser(null);PG.stop();
        for(var path:List.of("/api/v1/me/attempts","/api/v1/attempts/"+UUID.randomUUID())) {
            var r=send("GET",path,b,null);assertThat(r.statusCode()).isEqualTo(503);assertThat(r.body()).isEqualTo("{\"error\":\"unavailable\"}");
        }
        var cors=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/me/attempts"))
                .header("Origin","http://localhost:5173").header("Cookie",b.cookie()).timeout(Duration.ofSeconds(10)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(cors.statusCode()).isEqualTo(503);assertThat(cors.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(cors.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        assertThat(send("GET","/api/v1/status",null,null).statusCode()).isEqualTo(503);
        assertThat(send("GET","/api/v1/question-bank/catalog",null,null).statusCode()).isEqualTo(200);
    }
    String createBody(UUID operation,int quantity) { return "{\"commandId\":\""+operation+"\",\"roleId\":\"assistente-administrativo\",\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":"+quantity+",\"topicIds\":[]}] }"; }
    void assertSafe(String body) { assertThat(body).doesNotContain("correctAlternativeId","resolution","provenance","PRIVATE_BROWSER_RESOLUTION_SENTINEL","gabarito","percentage"); }
    Browser browser(String cookie) throws Exception {
        var r=send("GET","/api/v1/csrf",new Browser(cookie,null),null);
        if (cookie==null)cookie=r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        return new Browser(cookie,json.readTree(r.body()).get("token").asString());
    }
    Browser account(UUID id) throws Exception {
        jdbc.update("insert into app_user(id) values (?) on conflict do nothing",id);
        var s=repository().createSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(id),null,List.of())));
        repository().save(s);return browser("SESSION="+s.getId());
    }
    @SuppressWarnings({"unchecked","rawtypes"}) SessionRepository<Session> repository() { return (SessionRepository)sessions; }
    HttpResponse<String> send(String method,String path,Browser browser,String body) throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10))
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if(browser!=null&&browser.cookie()!=null)r.header("Cookie",browser.cookie());if(browser!=null&&browser.token()!=null)r.header("X-CSRF-TOKEN",browser.token());
        if(body!=null)r.header("Content-Type","application/json");
        var response=client.send(r.build(),HttpResponse.BodyHandlers.ofString());
        if(path.startsWith("/api/v1/attempts") || path.startsWith("/api/v1/me/attempts"))assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return response;
    }
    @SuppressWarnings("unchecked") void assertSchema(String body,String name) throws Exception {
        Map<String,Object> api=new org.yaml.snakeyaml.Yaml().load(java.nio.file.Files.readString(java.nio.file.Path.of("openapi.yaml")));
        var schemas=(Map<String,Object>)((Map<String,Object>)api.get("components")).get("schemas");check(json.readTree(body),(Map<String,Object>)schemas.get(name),schemas);
    }
    @SuppressWarnings("unchecked") void check(JsonNode value,Map<String,Object> schema,Map<String,Object> schemas) {
        if(schema.containsKey("$ref")){check(value,(Map<String,Object>)schemas.get(schema.get("$ref").toString().replace("#/components/schemas/","")),schemas);return;}
        var type=schema.get("type");if(type instanceof List<?> types){if(value.isNull()){assertThat(types.contains("null")).isTrue();return;}type=types.stream().filter(t -> !t.equals("null")).findFirst().orElseThrow();}
        switch(type.toString()) {
            case "object" -> {assertThat(value.isObject()).isTrue();var props=(Map<String,Object>)schema.get("properties");assertThat(schema.get("additionalProperties")).isEqualTo(false);
                var keys=new HashSet<String>();value.propertyNames().forEach(keys::add);assertThat(keys).containsExactlyInAnyOrderElementsOf(props.keySet());for(var key:keys)check(value.get(key),(Map<String,Object>)props.get(key),schemas);}
            case "array" -> {assertThat(value.isArray()).isTrue();for(var item:value)check(item,(Map<String,Object>)schema.get("items"),schemas);}
            case "string" -> assertThat(value.isString()).isTrue();
            case "integer" -> assertThat(value.isIntegralNumber()).isTrue();
            case "number" -> assertThat(value.isNumber()).isTrue();
            default -> throw new AssertionError("Tipo não coberto");
        }
    }
}

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
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    br.com.concursosimulator.mockexam.infrastructure.persistence.JpaCompletedAttempts evidence;
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
    @Test @Order(4) void allProjectionLevelsRejectPrivateFieldsAndReadsNeverWriteBusinessRows() throws Exception {
        var a=account(UUID.randomUUID());var v=create(a);var path="/api/v1/attempts/"+v.get("id").asString();
        send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        var before=rows();
        for(var entry:List.of(Map.entry(panel+role,"SubjectReviewOverview"),Map.entry(path+"/subject-review","SubjectReviewIndividual"))) {
            var response=send("GET",entry.getKey(),a,null);schema(response.body(),entry.getValue());
            assertThat(response.body()).doesNotContain("PRIVATE_BROWSER_RESOLUTION_SENTINEL","questionId","revisionId","itemId","owner","accountId","correctAlternativeId","resolution","parameters","Digest","generatorVersion");
            for(var target:List.of("root","subject")) {
                var value=json.readTree(response.body());var object=(tools.jackson.databind.node.ObjectNode)(target.equals("root")?value:value.get("subjects").get(0));
                object.put("parameters","PRIVATE_INJECTED");assertThatThrownBy(()->schema(value.toString(),entry.getValue())).isInstanceOf(AssertionError.class);
            }
        }
        assertThat(rows()).isEqualTo(before);
    }
    @Test @Order(5) void corruptionAndTechnicalFailuresReturnWholeGenericErrorsInsteadOfEmptySuccess() throws Exception {
        var a=account(UUID.randomUUID());var v=create(a);var path="/api/v1/attempts/"+v.get("id").asString();
        send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        jdbc.execute("alter table me_item disable trigger user");
        try { jdbc.update("update me_item set payload=jsonb_set(payload,'{question,revisionId}',to_jsonb(?::text)) where attempt_id=?",UUID.randomUUID().toString(),UUID.fromString(v.get("id").asString())); }
        finally { jdbc.execute("alter table me_item enable trigger user"); }
        var before=rows();
        for(var url:List.of(panel+role,path+"/subject-review")) {var r=send("GET",url,a,null);assertThat(r.statusCode()).isEqualTo(500);assertThat(r.body()).isEqualTo("{\"error\":\"internal_error\"}");}
        assertThat(rows()).isEqualTo(before);
        for(var failure:List.of(new IllegalStateException("PRIVATE_FORMAT payload=secret"),new org.springframework.dao.InvalidDataAccessApiUsageException("PRIVATE_INTEGRITY"),
                new org.springframework.dao.DataAccessResourceFailureException("PRIVATE_JDBC password=secret"),new org.springframework.transaction.CannotCreateTransactionException("PRIVATE_TX"))) {
            org.mockito.Mockito.doThrow(failure).when(evidence).recent(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.any());
            try {var r=send("GET",panel+role,a,null);boolean integrity=failure instanceof IllegalStateException || failure instanceof org.springframework.dao.InvalidDataAccessApiUsageException;
                assertThat(r.statusCode()).isEqualTo(integrity?500:503);assertThat(r.body()).isEqualTo(integrity?"{\"error\":\"internal_error\"}":"{\"error\":\"unavailable\"}");}
            finally {org.mockito.Mockito.reset(evidence);}
        }
        assertThat(rows()).isEqualTo(before);
    }
    @Test @Order(6) void finishedDuringHttpReadAppearsOnlyInTheFollowingWindowAndLogoutInvalidatesFollowingRequests() throws Exception {
        var a=account(UUID.randomUUID());var v=create(a);var path="/api/v1/attempts/"+v.get("id").asString();
        var captured=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var first=new java.util.concurrent.atomic.AtomicBoolean(true);
        org.mockito.Mockito.doAnswer(call->{var window=call.callRealMethod();if(first.getAndSet(false)){captured.countDown();if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Timeout fixture");}return window;})
                .when(evidence).recent(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.any());
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var pending=pool.submit(()->send("GET",panel+role,a,null));assertThat(captured.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}").statusCode()).isEqualTo(200);
            assertThat(send("POST","/api/v1/auth/logout",a,null).statusCode()).isEqualTo(204);release.countDown();
            var earlier=pending.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThat(earlier.statusCode()).isEqualTo(200);
            assertThat(json.readTree(earlier.body()).get("window").get("attemptCount").asInt()).isZero();
            assertThat(send("GET",panel+role,a,null).statusCode()).isEqualTo(401);
        } finally {release.countDown();org.mockito.Mockito.reset(evidence);}
        // Identity was checked when the earlier request entered. It is not a promise to revoke an in-flight response.
        var owner=jdbc.queryForObject("select owner_id from me_attempt where id=?",UUID.class,UUID.fromString(v.get("id").asString()));
        var fresh=account(owner);var later=json.readTree(send("GET",panel+role,fresh,null).body());
        assertThat(later.get("window").get("attemptCount").asInt()).isEqualTo(1);assertThat(later.get("subjects").get(0).get("blank").asInt()).isEqualTo(3);
        assertThat(send("GET",path+"/result",fresh,null).statusCode()).isEqualTo(200);
    }
    @Test @Order(7) void visitorExpiryIsCheckedForTheCapturedReadAndNextRequestCannotRecoverExpiredEvidence() throws Exception {
        var a=browser(null);var v=create(a);var path="/api/v1/attempts/"+v.get("id").asString();var id=UUID.fromString(v.get("id").asString());
        send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        var captured=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var first=new java.util.concurrent.atomic.AtomicBoolean(true);var now=Instant.now();
        var shortSession=repository().findById(a.cookie().substring(8));shortSession.setMaxInactiveInterval(Duration.ofSeconds(1));repository().save(shortSession);
        org.mockito.Mockito.doAnswer(call->{var result=call.callRealMethod();if(first.getAndSet(false)){captured.countDown();if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Timeout fixture");}return result;})
                .when(evidence).one(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(id),org.mockito.ArgumentMatchers.any());
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var pending=pool.submit(()->send("GET",path+"/subject-review",a,null));assertThat(captured.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            Thread.sleep(1300);release.countDown();
            assertThat(pending.get(10,java.util.concurrent.TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        } finally {release.countDown();org.mockito.Mockito.reset(evidence);}
        // The old actor still cannot read beyond the domain's 24h deadline even with a live session.
        var owner=jdbc.queryForObject("select owner_id from me_attempt where id=?",UUID.class,id);
        var actor=new br.com.concursosimulator.mockexam.domain.AttemptOwner(br.com.concursosimulator.mockexam.domain.AttemptOwner.Kind.VISITOR,owner);
        assertThatThrownBy(()->evidence.one(actor,id,now.plusSeconds(86401))).isInstanceOf(br.com.concursosimulator.mockexam.domain.Attempt.Unavailable.class);
        assertThat(send("GET",path+"/subject-review",a,null).statusCode()).isEqualTo(404);
        assertThat(send("GET",path+"/subject-review",browser(null),null).statusCode()).isEqualTo(404);
    }
    @Test @Order(8) void capturedGeneratedResultRemainsMinimalAfterTemplateWithdrawalAndCorsKeepsCredentialsControlled() throws Exception {
        var versions=br.com.concursosimulator.parameterization.ParameterizedBrowserFixtures.seed(context);
        var a=account(UUID.randomUUID());var body=createBody().replace("portugues","matematica-raciocinio").replace("\"quantity\":3","\"quantity\":4");
        body=body.substring(0,body.length()-1)+",\"useParameterizedQuestions\":true}";
        var created=send("POST","/api/v1/attempts",a,body);assertThat(created.statusCode()).isEqualTo(201);var id=json.readTree(created.body()).get("id").asString();
        var path="/api/v1/attempts/"+id;send("POST",path+"/finish",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}");
        var first=send("GET",path+"/subject-review",a,null);assertThat(first.statusCode()).isEqualTo(200);
        var uc=new br.com.concursosimulator.parameterization.application.TemplateUseCases(context.getBean(br.com.concursosimulator.parameterization.application.TemplateStore.class));
        for(var v:versions)uc.decide(br.com.concursosimulator.parameterization.ParameterFixtures.decision(v,br.com.concursosimulator.parameterization.domain.TemplateDecision.Action.WITHDRAW),1,2);
        var before=rows();assertThat(send("GET",path+"/subject-review",a,null).body()).isEqualTo(first.body());
        var response=send("GET",panel+role,a,null);schema(response.body(),"SubjectReviewOverview");
        assertThat(response.body()).doesNotContain("parameters","Digest","correctAlternativeId","resolution","generatorVersion","PRIVATE_");assertThat(rows()).isEqualTo(before);
        for(var origin:List.of("http://localhost:5173","https://unknown.example")) {
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+panel+role)).header("Cookie",a.cookie()).header("Origin",origin).build();
            var cors=client.send(request,HttpResponse.BodyHandlers.ofString());assertThat(cors.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
            assertThat(cors.statusCode()).isEqualTo(origin.contains("localhost")?200:403);
            if(origin.contains("localhost")){assertThat(cors.headers().firstValue("Access-Control-Allow-Origin")).contains(origin);assertThat(cors.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");}
            else assertThat(cors.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        }
    }
    @Test @Order(9) void creationInAnotherSessionBeforeTheFirstSelectReturnsACoherentWindowInsteadOf404() throws Exception {
        var owner=UUID.randomUUID();var firstSession=account(owner);var secondSession=account(owner);
        var old=create(firstSession);var oldPath="/api/v1/attempts/"+old.get("id").asString();
        assertThat(send("POST",oldPath+"/finish",firstSession,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}").statusCode()).isEqualTo(200);
        var captured=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var first=new java.util.concurrent.atomic.AtomicBoolean(true);var at=new java.util.concurrent.atomic.AtomicReference<Instant>();
        org.mockito.Mockito.doAnswer(call->{
            if(first.getAndSet(false)){at.set(call.getArgument(3));captured.countDown();if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Timeout fixture");}
            return call.callRealMethod();
        }).when(evidence).recent(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.any());
        try(var pool=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var pending=pool.submit(()->send("GET",panel+role,firstSession,null));assertThat(captured.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var concurrent=create(secondSession);assertThat(Instant.parse(concurrent.get("createdAt").asString())).isAfter(at.get());
            assertThat(send("POST","/api/v1/attempts/"+concurrent.get("id").asString()+"/finish",secondSession,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0}").statusCode()).isEqualTo(200);
            var before=rows();release.countDown();var response=pending.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(response.statusCode()).isEqualTo(200);var earlier=json.readTree(response.body());assertThat(earlier.get("window").get("attemptCount").asInt()).isEqualTo(1);assertThat(earlier.get("window").get("hasOlder").asBoolean()).isFalse();
            var later=send("GET",panel+role,firstSession,null);assertThat(later.statusCode()).isEqualTo(200);assertThat(json.readTree(later.body()).get("window").get("attemptCount").asInt()).isEqualTo(2);
            assertThat(send("GET","/api/v1/me",firstSession,null).statusCode()).isEqualTo(200);assertThat(rows()).isEqualTo(before);
        } finally {release.countDown();org.mockito.Mockito.reset(evidence);}
    }
    @Test @Order(90) void databaseAndSessionOutageRemain503JsonNoStoreIncludingCors() throws Exception {
        var a=account(UUID.randomUUID());PG.stop();
        for(var url:List.of(panel+role,"/api/v1/attempts/"+UUID.randomUUID()+"/subject-review")) {
            var r=send("GET",url,a,null);assertThat(r.statusCode()).isEqualTo(503);assertThat(r.body()).isEqualTo("{\"error\":\"unavailable\"}");
        }
    }
    List<List<String>> rows() { return List.of("app_user","qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command","pq_instance").stream()
            .map(t->jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList(); }
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

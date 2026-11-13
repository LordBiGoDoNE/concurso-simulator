package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.domain.*;
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
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("local") @Testcontainers
class LessonPracticeHttpTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
    }
    @Autowired ConfigurableApplicationContext context;@Autowired JdbcTemplate jdbc;@Autowired JdbcIndexedSessionRepository sessions;
    @Value("${local.server.port}") int port;
    final HttpClient client=HttpClient.newHttpClient();final JsonMapper json=JsonMapper.builder().build();
    record Browser(String cookie,String token) {}
    @BeforeEach void seed() {
        if(jdbc.queryForObject("select count(*) from qb_question",Integer.class)>0)return;
        var store=context.getBean(QuestionStore.class);
        for(int i=0;i<32;i++) {
            var c=br.com.concursosimulator.questionbank.QuestionFixtures.content(i<30?"PRACTICE_ONLY_"+i:"MOCK_ONLY_"+i);
            var q=Question.draft(UUID.randomUUID(),new QuestionOrigin(i<30?QuestionPurpose.PRACTICE_NAMESPACE:"http-mock-fixture","P01-P"+i),UUID.randomUUID(),c);
            store.transaction(s->{s.insert(q);return null;});
            for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE))store.transaction(s->{
                var loaded=s.loadForUpdate(q.id()).orElseThrow();loaded.decide(new EditorialDecision(action,q.id(),q.latest().id(),c.fingerprint(),"Synthetic isolated fixture",Instant.now(),Set.of(EditorialDecision.Check.values()),"Synthetic test only"));s.save(loaded);return null;
            });
        }
    }
    @Test void arbitraryDestinationQuestionIdsAndQuantityAreRejectedBeforeWriting() throws Exception {
        var b=browser(null);var before=jdbc.queryForObject("select count(*) from me_attempt",Integer.class);
        for(var extra:List.of("\"roleId\":\"assistente-administrativo\"","\"purpose\":\"SIMULADO\"","\"questionIds\":[\""+UUID.randomUUID()+"\"]","\"quantity\":20")) {
            var original=start(UUID.randomUUID());var body=original.substring(0,original.length()-1)+","+extra+"}";
            var response=send("POST","/api/v1/lesson-practices",b,body);
            assertThat(response.statusCode()).isEqualTo(400);assertThat(response.body()).doesNotContain("PRACTICE_ONLY_","MOCK_ONLY_");
        }
        var mock="{\"commandId\":\""+UUID.randomUUID()+"\",\"roleId\":\"assistente-administrativo\",\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":1,\"topicIds\":[]}],\"purpose\":\"PRATICA_AULA\"}";
        assertThat(send("POST","/api/v1/attempts",b,mock).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(before);
        assertThat(send("GET","/api/v1/lesson-practices/availability?topicId=P01&roleId=assistente-administrativo",b,null).statusCode()).isEqualTo(400);
        assertThat(send("GET","/api/v1/lesson-practices/availability?topicId=P01&topicId=P02",b,null).statusCode()).isEqualTo(400);
        assertThat(send("POST","/api/v1/attempts",b,"{\"commandId\":\""+UUID.randomUUID()+"\",\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":1,\"topicIds\":[]}]}").statusCode()).isEqualTo(400);
    }
    @Test void visitorStartsDedicatedPracticeWithoutCompositionAndSeesCorrectionOnlyAfterFinish() throws Exception {
        var b=browser(null);var command=UUID.randomUUID();var payload=start(command);
        assertThat(send("POST","/api/v1/lesson-practices",new Browser(b.cookie(),null),payload).statusCode()).isEqualTo(403);
        var availability=send("GET","/api/v1/lesson-practices/availability?topicId=P01",b,null);
        assertThat(availability.statusCode()).isEqualTo(200);assertThat(json.readTree(availability.body()).get("available").asInt()).isEqualTo(30);
        assertThat(json.readTree(availability.body()).get("maximum").asInt()).isEqualTo(5);
        var created=send("POST","/api/v1/lesson-practices",b,payload);assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("PRACTICE_ONLY_").doesNotContain("MOCK_ONLY_","correctAlternativeId","resolution","percentage");
        var view=json.readTree(created.body());assertThat(view.get("items").size()).isEqualTo(5);
        assertThat(view.has("roleId")).isFalse();assertThat(view.get("topicId").asString()).isEqualTo("P01");
        var again=send("POST","/api/v1/lesson-practices",b,payload);assertThat(again.statusCode()).isEqualTo(200);assertThat(again.body()).isEqualTo(created.body());
        var path="/api/v1/lesson-practices/"+view.get("id").asString();
        assertThat(send("GET",path+"/result",b,null).statusCode()).isEqualTo(409);
        assertThat(send("GET","/api/v1/attempts/"+view.get("id").asString(),b,null).statusCode()).isEqualTo(404);
        var last=view.get("items").get(4);var answer="{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":\""+last.get("alternatives").get(0).get("id").asString()+"\"}";
        assertThat(send("PUT",path+"/answers/"+last.get("id").asString(),b,answer).statusCode()).isEqualTo(200);
        var finish=send("POST",path+"/finish",b,"{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":1}");
        assertThat(finish.statusCode()).isEqualTo(200);assertThat(finish.body()).contains("correctAlternativeId","resolution");
        assertThat(json.readTree(finish.body()).get("result").get("blank").asInt()).isEqualTo(4);
        assertThat(send("GET","/api/v1/me/lesson-practices",b,null).statusCode()).isEqualTo(401);
    }
    @Test void historiesAndOwnershipAndCrossModeWritesRemainSeparateThroughRealSecurityChain() throws Exception {
        var a=account(UUID.randomUUID());var other=account(UUID.randomUUID());
        var practice=json.readTree(send("POST","/api/v1/lesson-practices",a,start(UUID.randomUUID())).body());
        var mock=send("POST","/api/v1/attempts",a,"{\"commandId\":\""+UUID.randomUUID()+"\",\"roleId\":\"assistente-administrativo\",\"disciplines\":[{\"disciplineId\":\"portugues\",\"quantity\":1,\"topicIds\":[]}]}");
        assertThat(mock.statusCode()).isEqualTo(201);assertThat(mock.body()).contains("MOCK_ONLY_").doesNotContain("PRACTICE_ONLY_");
        var mockId=json.readTree(mock.body()).get("id").asString();var id=practice.get("id").asString();var item=practice.get("items").get(0).get("id").asString();
        assertThat(send("GET","/api/v1/lesson-practices/"+mockId,a,null).statusCode()).isEqualTo(404);
        assertThat(send("GET","/api/v1/lesson-practices/"+id,other,null).statusCode()).isEqualTo(404);
        var answer="{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":null}";
        assertThat(send("PUT","/api/v1/attempts/"+id+"/answers/"+item,a,answer).statusCode()).isEqualTo(404);
        assertThat(send("PUT","/api/v1/lesson-practices/"+id+"/answers/"+item,other,answer).statusCode()).isEqualTo(404);
        var histories=send("GET","/api/v1/me/lesson-practices?page=0&size=1",a,null);
        assertThat(histories.statusCode()).isEqualTo(200);assertThat(histories.body()).contains(id,"topicId").doesNotContain(mockId);
        var simulations=send("GET","/api/v1/me/attempts?page=0&size=1",a,null);
        assertThat(simulations.body()).contains(mockId).doesNotContain(id);
        var publicPool=send("GET","/api/v1/question-bank/questions?page=0&size=20",null,null);
        assertThat(json.readTree(publicPool.body()).get("total").asInt()).isEqualTo(2);
        assertThat(publicPool.body()).doesNotContain("PRACTICE_ONLY_");
        assertThat(send("POST","/api/v1/lesson-practices",a,start(UUID.randomUUID()).replace("P01","P02")).statusCode()).isEqualTo(409);
    }
    @Test void historicalPracticeIsProjectedWithoutRoleAndCommandsRemainOperableThroughRealHttp() throws Exception {
        var id=UUID.randomUUID();var b=account(id);var owner=new br.com.concursosimulator.mockexam.domain.AttemptOwner(br.com.concursosimulator.mockexam.domain.AttemptOwner.Kind.ACCOUNT,id);
        var command=UUID.randomUUID();var old=TopicPracticeCompatibilityTests.legacy(context.getBean(br.com.concursosimulator.mockexam.application.AttemptStore.class),owner,command);
        var path="/api/v1/lesson-practices/"+old.id();
        var read=send("GET",path,b,null);assertThat(read.statusCode()).isEqualTo(200);
        var projected=json.readTree(read.body());assertThat(projected.has("roleId")).isFalse();assertThat(projected.get("topicId").asString()).isEqualTo("P01");
        assertThat(json.readTree(send("GET","/api/v1/me/lesson-practices",b,null).body()).get("items").get(0).has("roleId")).isFalse();
        assertThat(send("POST","/api/v1/lesson-practices",b,start(command)).statusCode()).isEqualTo(409);
        var item=old.items().getFirst();var answer="{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":0,\"alternativeId\":\""+item.optionOrder().getFirst()+"\"}";
        var saved=send("PUT",path+"/answers/"+item.id(),b,answer);assertThat(saved.statusCode()).isEqualTo(200);
        assertThat(send("PUT",path+"/answers/"+item.id(),b,answer).body()).isEqualTo(saved.body());
        var finish="{\"commandId\":\""+UUID.randomUUID()+"\",\"expectedVersion\":1}";
        var result=send("POST",path+"/finish",b,finish);assertThat(result.statusCode()).isEqualTo(200);
        assertThat(send("POST",path+"/finish",b,finish).body()).isEqualTo(result.body());
        assertThat(json.readTree(result.body()).get("attempt").has("roleId")).isFalse();
        var stored=json.readTree(jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,old.id()));
        assertThat(stored.get("format").asInt()).isEqualTo(3);assertThat(stored.get("composition").isObject()).isTrue();
        expire(b);
        assertThat(send("GET",path,b,null).statusCode()).isEqualTo(404);
        assertThat(send("POST",path+"/finish",b,finish).statusCode()).isEqualTo(403);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) void expire(Browser b) {
        SessionRepository<Session> repository=(SessionRepository)sessions;
        var session=repository.findById(b.cookie().substring("SESSION=".length()));
        session.setLastAccessedTime(Instant.now().minusSeconds(3600));repository.save(session);
    }
    String start(UUID id) {return "{\"commandId\":\""+id+"\",\"topicId\":\"P01\"}";}
    Browser browser(String cookie) throws Exception {
        var r=send("GET","/api/v1/csrf",new Browser(cookie,null),null);
        if(cookie==null)cookie=r.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        return new Browser(cookie,json.readTree(r.body()).get("token").asString());
    }
    @SuppressWarnings({"unchecked","rawtypes"}) Browser account(UUID id) throws Exception {
        jdbc.update("insert into app_user(id) values (?)",id);SessionRepository<Session> repository=(SessionRepository)sessions;
        var session=repository.createSession();session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(id),null,List.of())));repository.save(session);
        return browser("SESSION="+session.getId());
    }
    HttpResponse<String> send(String method,String path,Browser b,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if(b!=null && b.cookie()!=null)request.header("Cookie",b.cookie());if(b!=null && b.token()!=null)request.header("X-CSRF-TOKEN",b.token());if(body!=null)request.header("Content-Type","application/json");
        var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
        if(path.contains("lesson-practices"))assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");return response;
    }
}

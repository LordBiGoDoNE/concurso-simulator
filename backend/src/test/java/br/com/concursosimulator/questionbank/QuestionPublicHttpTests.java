package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.application.DecideQuestionUseCase;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionPublicHttpTests {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18.6").withUrlParam("connectTimeout","2").withUrlParam("socketTimeout","3");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Value("${local.server.port}") int port;
    @Autowired QuestionStore store;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoSpyBean JdbcIndexedSessionRepository sessions;
    @Autowired org.springframework.context.ApplicationContext context;
    @Autowired jakarta.servlet.ServletContext servlet;
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    static final UUID FIRST=UUID.fromString("10000000-0000-0000-0000-000000000001"),SECOND=UUID.fromString("20000000-0000-0000-0000-000000000002");
    static UUID firstRevision;
    HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/question-bank"+path)).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
    Question insert(UUID id,String statement,QuestionClassification classification) {
        var fixture=QuestionFixtures.content(statement);
        var content=new QuestionContent(fixture.statement(),fixture.alternatives(),fixture.correctAlternativeId(),fixture.resolution(),classification,fixture.references(),fixture.provenance());
        var q=Question.draft(id,new QuestionOrigin("fixture-http",id.toString()),UUID.randomUUID(),content);store.transaction(s -> {s.insert(q);return null;});return q;
    }
    void decision(UUID question,UUID revision,EditorialDecision.Action action) {
        var q=store.transaction(s -> s.loadForUpdate(question).orElseThrow());
        new DecideQuestionUseCase(store).execute(new EditorialDecision(action,question,revision,q.revision(revision).fingerprint(),"Fixture HTTP only",Instant.now(),Set.of(EditorialDecision.Check.values()),"Teste sintético isolado, não aprovação real"),q.eligible().map(QuestionRevision::id).orElse(null),q.editorialSequence());
    }
    @Test @Order(1) void catalogAndEmptyPageAreTypedPublicAndNoStore() throws Exception {
        var catalog=get("/catalog");assertThat(catalog.statusCode()).isEqualTo(200);
        assertThat(catalog.headers().firstValue("Cache-Control")).contains("no-store");
        var body=json.readTree(catalog.body());assertThat(body.get("disciplines").size()).isEqualTo(5);assertThat(body.get("topics").size()).isEqualTo(51);assertThat(body.get("roles").size()).isEqualTo(2);
        assertThat(body.get("contestId").asString()).isEqualTo("limeira-01-2026");
        var empty=get("/questions");assertThat(empty.statusCode()).isEqualTo(200);assertThat(json.readTree(empty.body()).get("total").asInt()).isZero();
        assertThat(json.readTree(empty.body()).get("items").size()).isZero();
        assertSchema(catalog.body(),"QuestionCatalog");assertSchema(empty.body(),"QuestionPage");
    }
    @Test @Order(2) void filtersAreAndOrderedPagedAndUseOnlyEligibleRevisionClassification() throws Exception {
        var q1=insert(FIRST,"Fixture pública: πr² e 2 < 3.",new QuestionClassification(Discipline.PORTUGUESE,Topic.P01,Set.of(TargetRole.values())));
        firstRevision=q1.latest().id();decision(FIRST,firstRevision,EditorialDecision.Action.SUBMIT);decision(FIRST,firstRevision,EditorialDecision.Action.APPROVE);
        var q2=insert(SECOND,"Outra fixture",new QuestionClassification(Discipline.MATHEMATICS_LOGIC,Topic.M01,Set.of(TargetRole.SCHOOL_SECRETARY)));
        decision(SECOND,q2.latest().id(),EditorialDecision.Action.SUBMIT);decision(SECOND,q2.latest().id(),EditorialDecision.Action.APPROVE);
        var privateQuestion=insert(UUID.randomUUID(),"PRIVATE_DRAFT_SENTINEL",new QuestionClassification(Discipline.PORTUGUESE,Topic.P01,Set.of(TargetRole.values())));
        assertThat(get("/questions/"+privateQuestion.id()).statusCode()).isEqualTo(404);
        store.transaction(s -> {var q=s.loadForUpdate(FIRST).orElseThrow();q.append(UUID.randomUUID(),q2.latest().content());s.save(q);return null;});
        var page=get("/questions?size=1");var body=json.readTree(page.body());assertThat(body.get("total").asInt()).isEqualTo(2);assertThat(body.get("items").get(0).get("questionId").asString()).isEqualTo(FIRST.toString());
        assertThat(json.readTree(get("/questions?page=1&size=1").body()).get("items").get(0).get("questionId").asString()).isEqualTo(SECOND.toString());
        var filtered=json.readTree(get("/questions?disciplineId=portugues&topicId=P01&roleId=secretario-de-escola").body());assertThat(filtered.get("total").asInt()).isEqualTo(1);
        assertThat(json.readTree(get("/questions?disciplineId=matematica-raciocinio&roleId=assistente-administrativo").body()).get("total").asInt()).isZero();
        var detail=get("/questions/"+FIRST);assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(json.readTree(detail.body()).get("revisionId").asString()).isEqualTo(firstRevision.toString());
        assertThat(json.readTree(detail.body()).get("classification").get("disciplineId").asString()).isEqualTo("portugues");
        assertThat(detail.body()).contains("πr²","2 < 3").doesNotContain("correctAlternativeId","resolution","status","provenance","checklist","gabarito","PRIVATE_DRAFT_SENTINEL");
        assertSchema(page.body(),"QuestionPage");assertSchema(detail.body(),"QuestionDetail");
        decision(SECOND,q2.latest().id(),EditorialDecision.Action.WITHDRAW);
        assertThat(get("/questions/"+SECOND).statusCode()).isEqualTo(404);
        assertThat(json.readTree(get("/questions").body()).get("total").asInt()).isEqualTo(1);
    }
    @Test @Order(3) void invalidParametersAndUnknownIdsHaveStable400404Contracts() throws Exception {
        for(var path:List.of("/questions?disciplineId=other","/questions?topicId=P99","/questions?roleId=admin","/questions?disciplineId=portugues&topicId=M01",
                "/questions?page=-1","/questions?size=0","/questions?size=101","/questions?page=999999999999","/questions?page=2147483647&size=100",
                "/questions?size=","/questions?size=1&size=2","/questions?answer=true","/catalog?extra=1","/questions/no-uuid","/questions/"+FIRST+"?revisionId=old")) {
            var response=get(path);assertThat(response.statusCode()).as(path).isEqualTo(400);assertThat(response.body()).isEqualTo("{\"error\":\"invalid_request\"}");
        }
        var absent=get("/questions/"+UUID.randomUUID());assertThat(absent.statusCode()).isEqualTo(404);assertThat(absent.body()).isEqualTo("{\"error\":\"not_found\"}");
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    @Test @Order(4) void publicAndDeniedRoutesNeverLoadRenewCreateOrCommitSessionsForAnyCookie() throws Exception {
        var repository=(org.springframework.session.SessionRepository<org.springframework.session.Session>)(org.springframework.session.SessionRepository)sessions;
        var session=repository.createSession();session.setLastAccessedTime(Instant.now().minusSeconds(70));
        session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new org.springframework.security.core.context.SecurityContextImpl(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        new br.com.concursosimulator.identity.web.UserPrincipal(UUID.randomUUID()),null,List.of())));
        repository.save(session);
        var invalid=repository.createSession();invalid.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        new org.springframework.security.core.context.SecurityContextImpl(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("not-a-UserPrincipal",null,List.of())));repository.save(invalid);
        var before=jdbc.queryForList("select session_id,last_access_time,expiry_time from spring_session order by session_id");
        var questions=jdbc.queryForList("select id,version,eligible_revision_id from qb_question order by id");
        var decisions=jdbc.queryForObject("select count(*) from qb_decision",Integer.class);
        var cookies=new ArrayList<String>();cookies.add(null);cookies.add("SESSION="+session.getId());
        cookies.add("SESSION="+Base64.getEncoder().encodeToString(session.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        cookies.add("SESSION="+invalid.getId());cookies.add("SESSION="+UUID.randomUUID());
        clearInvocations(sessions);
        for(var cookie:cookies) {
            for(var path:List.of("/catalog","/questions","/questions/"+FIRST)) {
                var response=send("GET",path,cookie,null);assertThat(response.statusCode()).isEqualTo(200);assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
                var publicBody=get(path).body();assertThat(response.body()).isEqualTo(publicBody);
                for(var method:List.of("POST","PUT","PATCH","DELETE","HEAD")) {
                    var denied=send(method,path,cookie,null);assertThat(denied.statusCode()).as(method+path).isEqualTo(403);assertThat(denied.headers().allValues("Set-Cookie")).isEmpty();
                }
            }
            for(var path:List.of("/admin","/catalog/","/questions/"+FIRST+"/revisions","/questions/"+FIRST+"/gabarito","/questions/"+FIRST+"/resolution")) {
                var denied=send("GET",path,cookie,null);assertThat(denied.statusCode()).as(path).isEqualTo(403);assertThat(denied.headers().allValues("Set-Cookie")).isEmpty();
            }
        }
        assertNoSessionIo();
        assertThat(jdbc.queryForList("select session_id,last_access_time,expiry_time from spring_session order by session_id")).isEqualTo(before);
        assertThat(jdbc.queryForList("select id,version,eligible_revision_id from qb_question order by id")).isEqualTo(questions);
        assertThat(jdbc.queryForObject("select count(*) from qb_decision",Integer.class)).isEqualTo(decisions);
        assertThat(context.getBeansOfType(org.springframework.session.web.http.SessionRepositoryFilter.class)).hasSize(1);
        var registration=context.getBean("sessionRepositoryFilterRegistration",org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean.class);
        var security=context.getBean("securityFilterChainRegistration",org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean.class);
        assertThat(registration.getOrder()).isEqualTo(org.springframework.session.web.http.SessionRepositoryFilter.DEFAULT_ORDER).isLessThan(security.getOrder());
        assertThat(servlet.getFilterRegistrations().keySet().stream().filter(n -> n.toLowerCase(Locale.ROOT).contains("session"))).hasSize(1);
    }
    @Test @Order(5) void corsAllowsOnlyReadFromExplicitOriginsAndPreflightDoesNotTouchSessions() throws Exception {
        clearInvocations(sessions);
        for(var path:List.of("/catalog","/questions","/questions/"+FIRST)) {
            var allowed=send("GET",path,"SESSION="+UUID.randomUUID(),"http://localhost:5173");
            assertThat(allowed.statusCode()).isEqualTo(200);assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
            assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
            assertThat(allowed.headers().allValues("Set-Cookie")).isEmpty();
            assertThat(send("GET",path,null,"https://unknown.example").statusCode()).isEqualTo(403);
            for(var method:List.of("GET","POST")) {
                var preflight=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/question-bank"+path)).timeout(Duration.ofSeconds(5))
                        .method("OPTIONS",HttpRequest.BodyPublishers.noBody()).header("Origin","http://localhost:5173").header("Access-Control-Request-Method",method).build(),HttpResponse.BodyHandlers.ofString());
                assertThat(preflight.statusCode()).isEqualTo(method.equals("GET")?200:403);assertThat(preflight.headers().allValues("Set-Cookie")).isEmpty();
            }
        }
        assertNoSessionIo();
    }
    @SuppressWarnings({"unchecked","rawtypes"}) void assertNoSessionIo() {
        var repository=(org.springframework.session.SessionRepository<org.springframework.session.Session>)(org.springframework.session.SessionRepository)sessions;
        verify(repository,never()).findById(anyString());verify(repository,never()).save(any());verify(repository,never()).deleteById(anyString());
    }
    @Test @Order(6) void incompatibleMarkupAndCorrectiveResourceOrUrlNeverReachPublicJson() throws Exception {
        var original=QuestionFixtures.content("Fixture, enunciado sem resposta");
        var resource=new TextContent.Resource("assets/correct-answer.svg","image/svg+xml","a".repeat(64),"Alternativa correta");
        var badCases=List.of(
                new QuestionContent(new TextContent("<span data-correct='true'>MARKUP_SENTINEL</span>"),original.alternatives(),original.correctAlternativeId(),original.resolution(),original.classification(),original.references(),original.provenance()),
                new QuestionContent(original.statement(),List.of(new QuestionContent.Alternative(QuestionFixtures.FIRST,new TextContent("Opção",List.of(resource))),original.alternatives().get(1)),original.correctAlternativeId(),original.resolution(),original.classification(),original.references(),original.provenance()),
                new QuestionContent(original.statement(),original.alternatives(),original.correctAlternativeId(),original.resolution(),original.classification(),List.of(new StudyReference("Estudo",URI.create("https://example.test/%67abarito.html"),null)),original.provenance()));
        for(var content:badCases) {
            var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("fixture-projection-negative",UUID.randomUUID().toString()),UUID.randomUUID(),content);
            store.transaction(s -> {s.insert(q);return null;});decision(q.id(),q.latest().id(),EditorialDecision.Action.SUBMIT);decision(q.id(),q.latest().id(),EditorialDecision.Action.APPROVE);
            var response=get("/questions/"+q.id());assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).isEqualTo("{\"error\":\"unavailable\"}");assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
        assertThat(get("/questions").statusCode()).isEqualTo(503); // markup em resumo também é rejeitado
    }
    @Test @Order(7) void negativeSerializationControlDetectsAnAddedAnswerEvenWhenNotVisibleInUi() throws Exception {
        var good=get("/questions/"+FIRST);assertSchema(good.body(),"QuestionDetail");
        var mutant=(tools.jackson.databind.node.ObjectNode)json.readTree(good.body());mutant.put("correctAlternativeId",QuestionFixtures.FIRST.toString());
        assertThatThrownBy(() -> assertSchema(json.writeValueAsString(mutant),"QuestionDetail")).isInstanceOf(AssertionError.class);
        mutant.remove("correctAlternativeId");mutant.put("resolution","PRIVATE_RESOLUTION_NEGATIVE_CONTROL");
        assertThatThrownBy(() -> assertSchema(json.writeValueAsString(mutant),"QuestionDetail")).isInstanceOf(AssertionError.class);
        // Campo privado serializado pelo domínio existe no servidor, mas não no DTO/JSON público.
        var content=store.transaction(s -> s.loadForUpdate(FIRST).orElseThrow().revision(firstRevision).content());
        assertThat(json.writeValueAsString(content)).contains("correctAlternativeId","resolution","provenance");
        assertThat(good.body()).doesNotContain("correctAlternativeId","resolution","provenance");
    }
    @Test @Order(99) void unavailableDatabaseCannotMasqueradeAsAnEmptyCollectionOrUnknownId() throws Exception {
        POSTGRES.stop();
        for(var path:List.of("/questions","/questions/"+FIRST)) {
            long start=System.nanoTime();var response=get(path);
            assertThat(response.statusCode()).isEqualTo(503);assertThat(response.body()).isEqualTo("{\"error\":\"unavailable\"}");
            assertThat(Duration.ofNanos(System.nanoTime()-start)).isLessThan(Duration.ofSeconds(8));
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
        assertThat(get("/catalog").statusCode()).isEqualTo(200); // catálogo fixo não inventa estado do banco
    }
    HttpResponse<String> send(String method,String path,String cookie,String origin) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/question-bank"+path)).timeout(Duration.ofSeconds(10)).method(method,HttpRequest.BodyPublishers.noBody());
        if(cookie!=null) request.header("Cookie",cookie);if(origin!=null) request.header("Origin",origin);
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    @SuppressWarnings("unchecked") void assertSchema(String body,String name) throws Exception {
        Map<String,Object> api=new org.yaml.snakeyaml.Yaml().load(java.nio.file.Files.readString(java.nio.file.Path.of("openapi.yaml")));
        var schemas=(Map<String,Object>)((Map<String,Object>)api.get("components")).get("schemas");
        check(json.readTree(body),(Map<String,Object>)schemas.get(name),schemas);
    }
    @SuppressWarnings("unchecked") void check(tools.jackson.databind.JsonNode value,Map<String,Object> schema,Map<String,Object> schemas) {
        if(schema.containsKey("$ref")) {check(value,(Map<String,Object>)schemas.get(schema.get("$ref").toString().replace("#/components/schemas/","")),schemas);return;}
        switch(schema.get("type").toString()) {
            case "object" -> {
                assertThat(value.isObject()).isTrue();var props=(Map<String,Object>)schema.get("properties");
                var keys=new HashSet<String>();value.propertyNames().forEach(keys::add);
                assertThat(schema.get("additionalProperties")).isEqualTo(false);assertThat(keys).containsExactlyInAnyOrderElementsOf(props.keySet());
                for(var key:keys) check(value.get(key),(Map<String,Object>)props.get(key),schemas);
            }
            case "array" -> {assertThat(value.isArray()).isTrue();for(var item:value) check(item,(Map<String,Object>)schema.get("items"),schemas);}
            case "string" -> assertThat(value.isString()).isTrue();
            case "integer" -> assertThat(value.isIntegralNumber()).isTrue();
            default -> throw new AssertionError("Tipo não testado");
        }
    }
}

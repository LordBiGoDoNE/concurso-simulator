package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.web.AdministrativeImportController.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
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
import static br.com.concursosimulator.questionbank.AdministrativeImportFixtures.document;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Testcontainers
class AdministrativeImportHttpTests {
    static final UUID OPERATOR=UUID.fromString("77777777-1111-2222-3333-444444444444");
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
        r.add("app.question-import.destination-id",()->"isolated-test");
        r.add("app.question-import.confirmation-key",()->Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions;@Autowired JdbcTemplate jdbc;
    @Autowired br.com.concursosimulator.questionbank.application.port.QuestionStore questions;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean br.com.concursosimulator.questionbank.application.ApplyAdministrativeQuestionsUseCase importer;
    @Autowired @Qualifier("lessonPracticeUseCases") br.com.concursosimulator.mockexam.application.AttemptUseCases practice;
    @org.junit.jupiter.api.BeforeEach void persistentOperatorFixture(){
        jdbc.update("insert into app_user(id) values (?) on conflict do nothing",OPERATOR);
        jdbc.update("update admin_registration set owner_id=?,must_change=false,enabled=true where id=1",OPERATOR);
    }
    final HttpClient http=HttpClient.newHttpClient();final JsonMapper json=JsonMapper.builder().build();
    final String prefix="/api/v1/admin/question-imports";
    @SuppressWarnings({"unchecked","rawtypes"}) String session(Object principal){
        SessionRepository<Session> repository=(SessionRepository)sessions;var session=repository.createSession();
        var authentication=UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of());
        if(principal instanceof UserPrincipal p && p.id().equals(OPERATOR))authentication.setDetails(new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence(jdbc.queryForObject("select auth_version from admin_registration where id=1",Long.class)));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new SecurityContextImpl(authentication));
        repository.save(session);return "SESSION="+session.getId();
    }
    HttpResponse<String> send(String method,String path,String cookie,String csrf,String body,String origin)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json")
                .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        if(cookie!=null)request.header("Cookie",cookie);if(csrf!=null)request.header("X-CSRF-TOKEN",csrf);if(origin!=null)request.header("Origin",origin);
        return http.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    String csrf(String cookie)throws Exception{return json.readTree(send("GET","/api/v1/csrf",cookie,null,null,null).body()).get("token").asString();}
    @Test void realSecurityChainRejectsVisitorInvalidPrincipalUnprivilegedAndCsrfBeforeParsing()throws Exception{
        var visitor=send("GET","/api/v1/csrf",null,null,null,null);var visitorCookie=visitor.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        var visitorToken=json.readTree(visitor.body()).get("token").asString();
        assertThat(send("POST",prefix+"/preview",visitorCookie,visitorToken,"{broken",null).statusCode()).isEqualTo(401);
        for(var principal:List.of("invalid-principal",new UserPrincipal(UUID.randomUUID()))){
            var cookie=session(principal);var response=send("POST",prefix+"/preview",cookie,csrf(cookie),"{broken",null);
            assertThat(response.statusCode()).isEqualTo(principal instanceof UserPrincipal?403:401);assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
        var operator=session(new UserPrincipal(OPERATOR));
        assertThat(send("POST",prefix+"/preview",operator,null,"{broken",null).statusCode()).isEqualTo(403);
        assertThat(send("POST",prefix+"/preview",operator,"invalid","{broken",null).statusCode()).isEqualTo(403);
        assertThat(send("POST",prefix+"/preview",operator,csrf(operator),"{broken","https://unknown.test").statusCode()).isEqualTo(403);
        assertThat(send("POST","/api/v1/question-bank/questions",operator,csrf(operator),"{}",null).statusCode()).isEqualTo(403);
        assertThat(send("GET","/api/v1/me",operator,null,null,null).statusCode()).isEqualTo(200);
    }
    @Test void bothCollectionsPreviewApplyAndReplayViaHttpWithoutLeakingContent()throws Exception{
        var cookie=session(new UserPrincipal(OPERATOR));var csrf=csrf(cookie);
        int editorialBefore=jdbc.queryForObject("select count(*) from qb_decision",Integer.class);
        for(var purpose:QuestionPurpose.values()) {
            var key="admin.http"+purpose;var doc=document(purpose,key);int before=jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class);
            var preview=send("POST",prefix+"/preview",cookie,csrf,json.writeValueAsString(new PreviewRequest(doc,List.of(key))),null);
            assertThat(preview.statusCode()).as(preview.body()).isEqualTo(200);assertThat(preview.headers().firstValue("Cache-Control")).contains("no-store");
            assertThat(preview.body()).doesNotContain("PRIVATE_SENTINEL","Resolução sintética","correctAlternativeId");
            assertThat(jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class)).isEqualTo(before);
            var token=json.readTree(preview.body()).get("confirmationToken").asString();var body=json.writeValueAsString(new ApplyRequest(doc,List.of(key),token));
            var applied=send("POST",prefix+"/apply",cookie,csrf,body,null);assertThat(applied.statusCode()).as(applied.body()).isEqualTo(200);
            assertThat(json.readTree(applied.body()).get("replayed").asBoolean()).isFalse();
            var retry=send("POST",prefix+"/apply",cookie,csrf,body,null);assertThat(retry.statusCode()).as(retry.body()).isEqualTo(200);
            assertThat(json.readTree(retry.body()).get("replayed").asBoolean()).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class)).isEqualTo(before+1);
            var id=json.readTree(applied.body()).path("receipt").path("results").get(0).path("questionId").asString();
            assertThat(send("GET","/api/v1/question-bank/questions/"+id,null,null,null,null).statusCode()).isEqualTo(404);
            var changed=document(purpose,"admin.different");
            var mismatch=send("POST",prefix+"/apply",cookie,csrf,json.writeValueAsString(new ApplyRequest(changed,List.of("admin.different"),token)),null);
            assertThat(mismatch.statusCode()).isEqualTo(409);
            assertThat(send("POST",prefix+"/apply",cookie,csrf,json.writeValueAsString(new ApplyRequest(doc,List.of(key),"X"+token)),null).statusCode()).isEqualTo(409);
        }
        assertThat(jdbc.queryForObject("select count(*) from qb_decision",Integer.class)).isEqualTo(editorialBefore);
    }
    @Test void strictJsonRejectsDuplicateTrailingUnknownAndMissingBasesSanitized()throws Exception{
        var cookie=session(new UserPrincipal(OPERATOR));var token=csrf(cookie);var doc=document(QuestionPurpose.PRATICA_AULA,"admin.strict");
        var good=json.writeValueAsString(new PreviewRequest(doc,List.of("admin.strict")));
        for(var bad:List.of(good+" {}","{\"package\":null,\"package\":null,\"keys\":[]}",good.replace("\"keys\":", "\"PRIVATE_SENTINEL\":"))) {
            var response=send("POST",prefix+"/preview",cookie,token,bad,null);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(400);assertThat(response.body()).doesNotContain("PRIVATE_SENTINEL","Fixture");
        }
        var missing=good.replace(",\"expectedLatestRevision\":null","");
        assertThat(send("POST",prefix+"/preview",cookie,token,missing,null).statusCode()).isEqualTo(422);
        var wrong=good.replace("\"purpose\":\"PRATICA_AULA\"","\"purpose\":\"SIMULADO\"");
        assertThat(send("POST",prefix+"/preview",cookie,token,wrong,null).statusCode()).isEqualTo(422);
    }
    @Test @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void unexpectedAndTransientFailuresHaveStableSanitizedContracts(org.springframework.boot.test.system.CapturedOutput output)throws Exception {
        var cookie=session(new UserPrincipal(OPERATOR));var csrf=csrf(cookie);var d=document(QuestionPurpose.SIMULADO,"admin.failed");
        var preview=send("POST",prefix+"/preview",cookie,csrf,json.writeValueAsString(new PreviewRequest(d,List.of("admin.failed"))),null);
        var token=json.readTree(preview.body()).path("confirmationToken").asString();
        var body=json.writeValueAsString(new ApplyRequest(d,List.of("admin.failed"),token));
        int before=jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class);
        for(var failure:List.of(new IllegalStateException("PRIVATE_FAILURE_SENTINEL"),new org.springframework.dao.TransientDataAccessResourceException("PRIVATE_FAILURE_SENTINEL"))){
            org.mockito.Mockito.doThrow(failure).when(importer).execute(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString());
            var response=send("POST",prefix+"/apply",cookie,csrf,body,null);
            assertThat(response.statusCode()).isEqualTo(failure instanceof IllegalStateException?500:503);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            assertThat(response.body()).doesNotContain("PRIVATE_FAILURE_SENTINEL",token,"correctAlternativeId");
        }
        assertThat(output).doesNotContain("PRIVATE_FAILURE_SENTINEL");
        assertThat(jdbc.queryForObject("select count(*) from qb_import_receipt",Integer.class)).isEqualTo(before);
    }
    @Test void stalePreviewNeverIssuesConfirmationOrReservesIdentity()throws Exception {
        var cookie=session(new UserPrincipal(OPERATOR));var token=csrf(cookie);var d=document(QuestionPurpose.SIMULADO,"admin.stale-preview");
        var item=d.items().getFirst();var staleItem=new br.com.concursosimulator.questionbank.application.port.QuestionImportReader.Item(item.origin(),item.content(),item.fingerprint(),UUID.randomUUID());
        var stale=new br.com.concursosimulator.questionbank.application.port.QuestionImportReader.Document(d.format(),d.purpose(),List.of(staleItem),d.files());
        var response=send("POST",prefix+"/preview",cookie,token,json.writeValueAsString(new PreviewRequest(stale,List.of(item.origin().key()))),null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);var body=json.readTree(response.body());
        assertThat(body.get("confirmable").asBoolean()).isFalse();assertThat(body.get("confirmationToken").isNull()).isTrue();
        assertThat(body.path("items").get(0).path("change").asString()).isEqualTo("STALE");
        assertThat(jdbc.queryForObject("select count(*) from qb_question where origin_key='admin.stale-preview'",Integer.class)).isZero();
    }
    @Test void newPracticeEntriesGrowPoolAndAreSelectedOnlyAfterSyntheticApproval()throws Exception {
        var cookie=session(new UserPrincipal(OPERATOR));var csrf=csrf(cookie);
        var keys=java.util.stream.IntStream.range(0,6).mapToObj(n->"admin.grow"+n).toArray(String[]::new);
        var doc=document(QuestionPurpose.PRATICA_AULA,keys);
        var preview=send("POST",prefix+"/preview",cookie,csrf,json.writeValueAsString(new PreviewRequest(doc,List.of(keys))),null);
        var token=json.readTree(preview.body()).path("confirmationToken").asString();
        var applied=send("POST",prefix+"/apply",cookie,csrf,json.writeValueAsString(new ApplyRequest(doc,List.of(keys),token)),null);
        assertThat(applied.statusCode()).as(applied.body()).isEqualTo(200);
        var ids=new HashSet<UUID>();
        for(var result:json.readTree(applied.body()).path("receipt").path("results"))ids.add(UUID.fromString(result.path("questionId").asString()));
        for(var id:ids)for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE))questions.transaction(s->{
            var q=s.loadForUpdate(id).orElseThrow();q.decide(new EditorialDecision(action,q.id(),q.latest().id(),q.latest().fingerprint(),"Synthetic test only",java.time.Instant.now(),Set.of(EditorialDecision.Check.values()),"Not editorial approval of real material"));s.save(q);return null;
        });
        var available=practice.practiceAvailability(TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01);
        assertThat(available.available()).isEqualTo(6);assertThat(available.maximum()).isEqualTo(5);
        var started=practice.createPractice(new br.com.concursosimulator.mockexam.domain.AttemptOwner(br.com.concursosimulator.mockexam.domain.AttemptOwner.Kind.VISITOR,UUID.randomUUID()),UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt();
        assertThat(started.items()).hasSize(5).allSatisfy(i->assertThat(ids).contains(i.questionId()));
        assertThat(practice.practiceAvailability(TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P02).available()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from qb_question where id in (select question_id from qb_revision where status='APPROVED') and purpose='SIMULADO'",Integer.class)).isZero();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"nullKeys","numberKey","nullElement","nullToken"})
    void invalidEnvelopeFieldTypesAre400(String mode)throws Exception {
        var cookie=session(new UserPrincipal(OPERATOR));var token=csrf(cookie);var doc=document(QuestionPurpose.SIMULADO,"123");
        String body=json.writeValueAsString(new PreviewRequest(doc,List.of("123")));
        String path=prefix+"/preview";
        if(mode.equals("nullKeys"))body=body.replace("\"keys\":[\"123\"]","\"keys\":null");
        if(mode.equals("nullElement"))body=body.replace("\"keys\":[\"123\"]","\"keys\":[null]");
        if(mode.equals("numberKey"))body=body.replace("\"keys\":[\"123\"]","\"keys\":[123]");
        if(mode.equals("nullToken")){body=json.writeValueAsString(new ApplyRequest(doc,List.of("123"),null));path=prefix+"/apply";}
        var response=send("POST",path,cookie,token,body,null);
        assertThat(response.statusCode()).as(mode+": "+response.body()).isEqualTo(400);
    }
    @Test void otherJsonMediaTypesAre415()throws Exception {
        var cookie=session(new UserPrincipal(OPERATOR));var token=csrf(cookie);var doc=document(QuestionPurpose.SIMULADO,"admin.media");
        for(var mediaType:List.of("application/problem+json","application/json; charset=ISO-8859-1")){
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+prefix+"/preview"))
            .header("Cookie",cookie).header("X-CSRF-TOKEN",token).header("Content-Type",mediaType)
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(new PreviewRequest(doc,List.of("admin.media"))))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(mediaType).isEqualTo(415);assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
    }

}

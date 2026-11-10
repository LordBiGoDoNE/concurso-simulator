package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.application.*;
import br.com.concursosimulator.identity.application.port.AdministrativePasswords;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Import(OidcTestRegistration.class) @Testcontainers @ExtendWith(OutputCaptureExtension.class)
class AdministrativeAccountHttpTests {
    static final OidcFixture OIDC=new OidcFixture();
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    static final String PASSWORD="synthetic-administrative-password";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
        r.add("app.google.enabled",()->true);r.add("fixture.issuer",OIDC::issuer);
        r.add("app.google.callback-url",()->"{baseUrl}/login/oauth2/code/google");r.add("app.google.frontend-url",()->"http://127.0.0.1:5173");
    }
    @AfterAll static void close(){OIDC.close();}
    @Value("${local.server.port}")int port;
    @Autowired JdbcTemplate jdbc;@Autowired AdministrativeAccessUseCases access;@Autowired AdministrativePasswords passwords;
    @Autowired org.springframework.session.jdbc.JdbcIndexedSessionRepository sessions;
    UUID local;
    final JsonMapper json=JsonMapper.builder().build();
    @BeforeEach void isolatedCredential(){
        // Reject every intermediate JDBC write, including sendRedirect's early session commit.
        // A final-row assertion misses tokens written and subsequently removed by the handler.
        jdbc.execute("""
                create or replace function reject_oauth_session_secrets() returns trigger language plpgsql as $$
                begin
                    if position('fixture-access-'::bytea in NEW.attribute_bytes)>0
                        or position('fixture-refresh-'::bytea in NEW.attribute_bytes)>0
                        or position('OAuth2AuthorizedClient'::bytea in NEW.attribute_bytes)>0
                        or position('OidcIdToken'::bytea in NEW.attribute_bytes)>0 then
                        raise exception 'OAuth secrets must never reach the session store' using errcode='23514';
                    end if;
                    return NEW;
                end $$
                """);
        jdbc.execute("drop trigger if exists reject_oauth_session_secrets on spring_session_attributes");
        jdbc.execute("create trigger reject_oauth_session_secrets before insert or update on spring_session_attributes for each row execute function reject_oauth_session_secrets()");
        local=UUID.randomUUID();jdbc.update("insert into app_user(id) values (?)",local);
        jdbc.update("update admin_registration set owner_id=?,password_hash=?,must_change=false,enabled=true,auth_version=auth_version+1,failed_attempts=0,failure_window=null,blocked_until=null",local,passwords.encode(PASSWORD));
        OIDC.mode="valid";OIDC.subject="http-"+UUID.randomUUID();
    }
    String base(){return "http://127.0.0.1:"+port;}
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(path.startsWith("http")?path:base()+path)).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    HttpResponse<String> post(HttpClient c,String path,String body,boolean csrf)throws Exception {
        var r=HttpRequest.newBuilder(URI.create(base()+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json");
        if(csrf)r.header("X-CSRF-TOKEN",json.readTree(get(c,"/api/v1/csrf").body()).get("token").asString());
        return c.send(r.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> login(HttpClient c)throws Exception{return post(c,"/api/v1/admin/auth/login",json.writeValueAsString(Map.of("username","admin","password",PASSWORD)),true);}
    String callback(HttpClient c,String start)throws Exception {
        var authorization=get(c,start);assertThat(authorization.statusCode()).as(authorization.body()).isEqualTo(302);
        var atProvider=get(c,authorization.headers().firstValue("Location").orElseThrow());
        return atProvider.headers().firstValue("Location").orElseThrow();
    }
    String id(HttpClient c)throws Exception{return json.readTree(get(c,"/api/v1/me").body()).get("id").asString();}
    @Test void localLoginRotatesSessionAndPersistsOnlyAdaptiveHash(CapturedOutput output)throws Exception {
        var c=browser();get(c,"/api/v1/csrf");var cookies=(CookieManager)c.cookieHandler().orElseThrow();String before=cookies.getCookieStore().getCookies().getFirst().getValue();
        var response=login(c);assertThat(response.statusCode()).as(response.body()).isEqualTo(204);
        String after=cookies.getCookieStore().getCookies().getFirst().getValue();assertThat(after).isNotEqualTo(before);
        assertThat(id(c)).isEqualTo(local.toString());
        var account=get(c,"/api/v1/admin/account");assertThat(account.statusCode()).isEqualTo(200);assertThat(account.body()).contains("admin","mustChangePassword").doesNotContain(PASSWORD,"passwordHash","auth_version");
        assertThat(account.headers().firstValue("Cache-Control")).contains("no-store");
        var hash=jdbc.queryForObject("select password_hash from admin_registration",String.class);assertThat(hash).startsWith("{pbkdf2-sha256-600k}").doesNotContain(PASSWORD);
        for(var bytes:jdbc.queryForList("select attribute_bytes from spring_session_attributes",byte[].class))assertThat(new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain(PASSWORD,hash,"AdministrativeAccount","OidcIdToken");
        assertThat(output.getAll()).doesNotContain(PASSWORD,hash);
        assertThat(access.bootstrap("changed","short",false)).isFalse();
    }
    @Test void realSecurityRejectsCsrfAnonymousPrincipalAndStrictBodyBeforeActions()throws Exception {
        var c=browser();
        assertThat(get(c,"/api/v1/admin/account").statusCode()).isEqualTo(401);
        assertThat(post(c,"/api/v1/admin/auth/login","{}",false).statusCode()).isEqualTo(403);
        for(var body:List.of("{\"username\":\"admin\",\"username\":\"admin\",\"password\":\"x\"}","{\"username\":\"admin\",\"password\":12}","{\"username\":\"admin\",\"password\":\"x\",\"extra\":true}","{} {}"))
            assertThat(post(c,"/api/v1/admin/auth/login",body,true).statusCode()).as(body).isEqualTo(400);
        assertThat(post(c,"/api/v1/admin/auth/login"," ".repeat(8193),true).statusCode()).isEqualTo(413);
        assertThat(jdbc.queryForObject("select failed_attempts from admin_registration",Integer.class)).isZero();
        assertThat(login(c).statusCode()).isEqualTo(204);
        assertThat(post(c,"/api/v1/admin/account/password","{}",false).statusCode()).isEqualTo(403);
        var csrf=json.readTree(get(c,"/api/v1/csrf").body()).get("token").asString();
        var cors=c.send(HttpRequest.newBuilder(URI.create(base()+"/api/v1/admin/account/password")).header("Origin","https://untrusted.example").header("X-CSRF-TOKEN",csrf).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(cors.statusCode()).isEqualTo(403);
        jdbc.update("update admin_registration set enabled=false");
        assertThat(get(c,"/api/v1/admin/account").statusCode()).isEqualTo(403);
    }
    @Test void securityRejectsUntrustedAnonymousAndObsoleteEvidenceBeforeAccountController()throws Exception {
        var valid=new br.com.concursosimulator.identity.web.UserPrincipal(local);
        long version=jdbc.queryForObject("select auth_version from admin_registration",Long.class);
        var malformed=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("untrusted",null,List.of());
        var unauthenticated=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated(valid,null);
        var noEvidence=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(valid,null,List.of());
        var old=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(valid,null,List.of());old.setDetails(new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence(version-1));
        var anonymous=new org.springframework.security.authentication.AnonymousAuthenticationToken("fixture-key",valid,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));anonymous.setDetails(new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence(version));
        @SuppressWarnings({"rawtypes","unchecked"}) org.springframework.session.SessionRepository<org.springframework.session.Session> repository=(org.springframework.session.SessionRepository)sessions;
        for(var auth:List.of(malformed,unauthenticated,noEvidence,old,anonymous)) {
            var session=repository.createSession();session.setAttribute(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,new org.springframework.security.core.context.SecurityContextImpl(auth));repository.save(session);
            var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base()+"/api/v1/admin/account")).header("Cookie","SESSION="+session.getId()).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(auth.getClass().getSimpleName()).isEqualTo(auth==noEvidence || auth==old?403:401);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
    }
    @Test void recoveryForcesChangeInvalidatesOldAdminAccessAndKeepsStudySession()throws Exception {
        var old=browser();assertThat(login(old).statusCode()).isEqualTo(204);
        access.recover("synthetic-temporary-password");var c=browser();
        assertThat(post(c,"/api/v1/admin/auth/login",json.writeValueAsString(Map.of("username","admin","password","synthetic-temporary-password")),true).statusCode()).isEqualTo(204);
        assertThat(get(c,"/api/v1/admin/account").body()).contains("\"mustChangePassword\":true");
        assertThat(post(c,"/api/v1/attempts","{}",true).statusCode()).isEqualTo(403);
        assertThat(post(c,"/api/v1/admin/account/google-link",json.writeValueAsString(Map.of("currentPassword","synthetic-temporary-password")),true).statusCode()).isEqualTo(403);
        assertThat(post(c,"/api/v1/admin/account/password",json.writeValueAsString(Map.of("currentPassword","synthetic-temporary-password","newPassword",PASSWORD)),true).statusCode()).isEqualTo(204);
        assertThat(id(c)).isEqualTo(local.toString());assertThat(get(c,"/api/v1/admin/account").body()).contains("\"mustChangePassword\":false");
        assertThat(get(old,"/api/v1/admin/account").statusCode()).isEqualTo(403);
        assertThat(get(old,"/api/v1/me").statusCode()).isEqualTo(200);
    }
    @Test void googleWithExistingHistoryIsPreservedAndBothLoginsResolveItsUuid()throws Exception {
        var google=browser();var subject="existing-"+UUID.randomUUID();OIDC.subject=subject;
        get(google,callback(google,"/oauth2/authorization/google"));var target=UUID.fromString(id(google));
        jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,payload) values (?,'ACCOUNT',?,?,now(),'{\"version\":0}')",UUID.randomUUID(),target,target);
        var history=jdbc.queryForList("select * from me_attempt where account_id=?",target);
        var admin=browser();assertThat(login(admin).statusCode()).isEqualTo(204);
        var started=post(admin,"/api/v1/admin/account/google-link",json.writeValueAsString(Map.of("currentPassword",PASSWORD)),true);
        assertThat(started.statusCode()).isEqualTo(200);var path=json.readTree(started.body()).get("authorizationUrl").asString();
        var completed=get(admin,callback(admin,path));assertThat(completed.headers().firstValue("Location")).contains("http://127.0.0.1:5173?admin=linked");
        assertThat(id(admin)).isEqualTo(target.toString());assertThat(get(admin,"/api/v1/admin/account").body()).contains("\"googleLinked\":true");
        assertThat(get(google,"/api/v1/admin/account").statusCode()).isEqualTo(403); // old student session did not acquire an epoch
        var byPassword=browser();assertThat(login(byPassword).statusCode()).isEqualTo(204);assertThat(id(byPassword)).isEqualTo(target.toString());
        var byGoogle=browser();get(byGoogle,callback(byGoogle,"/oauth2/authorization/google"));assertThat(id(byGoogle)).isEqualTo(target.toString());assertThat(get(byGoogle,"/api/v1/admin/account").statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForList("select * from me_attempt where account_id=?",target)).isEqualTo(history);
        assertThat(jdbc.queryForObject("select active from app_user where id=?",Boolean.class,local)).isFalse();
    }
    @Test void localHistoryConflictNeverPromotesOrMerges()throws Exception {
        var normal=browser();OIDC.subject="conflict-"+UUID.randomUUID();get(normal,callback(normal,"/oauth2/authorization/google"));String target=id(normal);
        jdbc.update("insert into qb_import_receipt values (?,'fixture',?,now(),'{}')",UUID.randomUUID(),local);
        var admin=browser();login(admin);post(admin,"/api/v1/admin/account/google-link",json.writeValueAsString(Map.of("currentPassword",PASSWORD)),true);
        var response=get(admin,callback(admin,"/oauth2/authorization/google"));assertThat(response.headers().firstValue("Location")).contains("http://127.0.0.1:5173?admin=LOCAL_ACCOUNT_HAS_HISTORY");
        assertThat(id(admin)).isEqualTo(local.toString());assertThat(id(normal)).isEqualTo(target);
        assertThat(jdbc.queryForObject("select owner_id from admin_registration",UUID.class)).isEqualTo(local);
    }
    @Test void linkIntentIsBoundToStateSessionEpochAndExpiryAndCallbackCannotBeReplayed()throws Exception {
        for(var mode:List.of("expiry","epoch","session","state","cancel","valid")) {
            OIDC.mode="valid";OIDC.subject="bound-"+UUID.randomUUID();
            var admin=browser();login(admin);
            post(admin,"/api/v1/admin/account/google-link",json.writeValueAsString(Map.of("currentPassword",PASSWORD)),true);
            String callback=callback(admin,"/oauth2/authorization/google");
            var cookies=(CookieManager)admin.cookieHandler().orElseThrow();String id=cookies.getCookieStore().getCookies().getFirst().getValue();
            if(mode.equals("expiry")) {
                @SuppressWarnings({"unchecked","rawtypes"}) org.springframework.session.SessionRepository<org.springframework.session.Session> repository=(org.springframework.session.SessionRepository)sessions;
                var session=repository.findById(id);
                var intent=(br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.LinkIntent)session.getAttribute(br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.LINK);
                session.setAttribute(br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.LINK,new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.LinkIntent(intent.actor(),intent.version(),java.time.Instant.now().minusSeconds(1),intent.state()));repository.save(session);
            }
            if(mode.equals("epoch"))jdbc.update("update admin_registration set auth_version=auth_version+1");
            if(mode.equals("state"))callback=callback.replaceAll("state=[^&]+","state=other-state");
            // OIDC mode is captured at authorization, so use an explicit provider cancellation callback for this branch.
            if(mode.equals("cancel"))callback=callback.replaceAll("code=[^&]+","error=access_denied");
            var result=get(mode.equals("session")?browser():admin,callback);
            if(mode.equals("valid")) {
                assertThat(result.headers().firstValue("Location")).contains("http://127.0.0.1:5173?admin=linked");
                var replay=get(admin,callback);assertThat(replay.headers().firstValue("Location")).contains("http://127.0.0.1:5173?auth=failed");
            } else {
                assertThat(result.headers().firstValue("Location").orElse("")).doesNotContain("admin=linked");
                assertThat(jdbc.queryForObject("select count(*) from external_identity where subject=?",Integer.class,OIDC.subject)).isZero();
            }
        }
    }
    @Test void repeatedFailuresReturnGenericErrorsAndPersistentThrottle()throws Exception {
        var c=browser();String invalid=json.writeValueAsString(Map.of("username","unknown","password",PASSWORD));
        for(int i=0;i<5;i++){var response=post(c,"/api/v1/admin/auth/login",invalid,true);assertThat(response.statusCode()).isEqualTo(401);assertThat(response.body()).isEqualTo("{\"error\":\"INVALID_CREDENTIALS\"}");}
        var throttled=login(c);assertThat(throttled.statusCode()).isEqualTo(429);assertThat(throttled.headers().firstValue("Retry-After")).contains("900");
        assertThat(get(c,"/api/v1/me").statusCode()).isEqualTo(401);
    }
}

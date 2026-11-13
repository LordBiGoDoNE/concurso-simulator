package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.web.AdministrativeImportController.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static br.com.concursosimulator.questionbank.AdministrativeImportFixtures.document;
import static org.assertj.core.api.Assertions.*;

/** Consulta administrativa (#177) pela cadeia HTTP/segurança real e PostgreSQL isolado; dados sintéticos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Testcontainers
@TestMethodOrder(MethodOrderer.MethodName.class)
class AdministrativeQuestionHttpTests {
    static final UUID OPERATOR = UUID.fromString("88888888-1111-2222-3333-444444444444");
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl); r.add("spring.datasource.username", PG::getUsername); r.add("spring.datasource.password", PG::getPassword);
        r.add("app.question-import.destination-id", () -> "isolated-test");
        r.add("app.question-import.confirmation-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions; @Autowired JdbcTemplate jdbc;
    @Autowired br.com.concursosimulator.questionbank.application.port.QuestionStore questions;
    @Autowired AdministrativePackageReader reader;
    final HttpClient http = HttpClient.newHttpClient(); final JsonMapper json = JsonMapper.builder().build();
    static final String BASE = "/api/v1/admin/questions";

    @BeforeEach void operator() {
        jdbc.update("insert into app_user(id) values (?) on conflict do nothing", OPERATOR);
        jdbc.update("update admin_registration set owner_id=?,must_change=false,enabled=true where id=1", OPERATOR);
    }
    @SuppressWarnings({"unchecked", "rawtypes"}) String session(Object principal, Long version) {
        SessionRepository<Session> repository = (SessionRepository) sessions; var session = repository.createSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        if (version != null) authentication.setDetails(new br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence(version));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(authentication));
        repository.save(session); return "SESSION=" + session.getId();
    }
    long version() { return jdbc.queryForObject("select auth_version from admin_registration where id=1", Long.class); }
    String admin() { return session(new UserPrincipal(OPERATOR), version()); }
    HttpResponse<String> send(String method, String path, String cookie, String csrf, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(30))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) request.header("Content-Type", "application/json");
        if (cookie != null) request.header("Cookie", cookie); if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    String csrf(String cookie) throws Exception { return json.readTree(send("GET", "/api/v1/csrf", cookie, null, null).body()).get("token").asString(); }
    JsonNode ok(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readTree(response.body());
    }
    /** Importa pelo fluxo administrativo real e devolve o corpo do recibo. */
    JsonNode imported(String cookie, String csrf, QuestionPurpose purpose, String... keys) throws Exception {
        var doc = document(purpose, keys);
        var preview = ok(send("POST", "/api/v1/admin/question-imports/preview", cookie, csrf, json.writeValueAsString(new PreviewRequest(doc, List.of(keys)))));
        return ok(send("POST", "/api/v1/admin/question-imports/apply", cookie, csrf,
                json.writeValueAsString(new ApplyRequest(doc, List.of(keys), preview.get("confirmationToken").asString())))).path("receipt");
    }
    void decide(UUID id, EditorialDecision.Action action, EditorialDecision.ReviewKind kind) {
        questions.transaction(s -> { var q = s.loadForUpdate(id).orElseThrow();
            q.decide(new EditorialDecision(action, id, q.latest().id(), q.latest().fingerprint(), "Fixture sintética", java.time.Instant.now(),
                    Set.of(EditorialDecision.Check.values()), "Somente teste isolado", kind)); s.save(q); return null; });
    }
    int writes() {
        return jdbc.queryForObject("select (select count(*) from qb_decision)+(select count(*) from qb_revision)+(select count(*) from qb_import_receipt)+(select count(*) from qb_question)", Integer.class);
    }

    @Test void a_realSecurityChainAdmitsOnlyValidAdministratorAndPublicRoutesStayUnchanged() throws Exception {
        var visitor = send("GET", BASE, null, null, null);
        assertThat(visitor.statusCode()).isEqualTo(401); assertThat(visitor.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(send("GET", BASE, session("invalid-principal", null), null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", BASE, "SESSION=" + UUID.randomUUID(), null, null).statusCode()).isEqualTo(401); // sessão inexistente/expirada
        var common = session(new UserPrincipal(UUID.randomUUID()), null);
        assertThat(send("GET", BASE, common, null, null).statusCode()).isEqualTo(403);
        var stale = session(new UserPrincipal(OPERATOR), version() - 1); // credencial revogada por troca de versão
        assertThat(send("GET", BASE, stale, null, null).statusCode()).isEqualTo(403);
        var pendingCookie = admin();
        jdbc.update("update admin_registration set must_change=true where id=1");
        assertThat(send("GET", BASE, pendingCookie, null, null).statusCode()).isEqualTo(403);
        jdbc.update("update admin_registration set must_change=false,enabled=false where id=1");
        assertThat(send("GET", BASE, pendingCookie, null, null).statusCode()).isEqualTo(403);
        jdbc.update("update admin_registration set enabled=true where id=1");
        var cookie = admin();
        for (var denied : List.of(send("GET", BASE, common, null, null), send("POST", BASE + "/lookup", common, csrf(common), "{\"items\":[]}")))
            assertThat(denied.body()).doesNotContain("questionId", "origin");
        ok(send("GET", BASE, cookie, null, null));
        // CSRF obrigatório em POST mesmo para ADM válido; métodos/rotas fora da lista são negados.
        assertThat(send("POST", BASE + "/lookup", cookie, null, "{\"items\":[]}").statusCode()).isEqualTo(403);
        assertThat(send("DELETE", BASE + "/" + UUID.randomUUID(), cookie, csrf(cookie), null).statusCode()).isEqualTo(403);
        assertThat(send("POST", BASE, cookie, csrf(cookie), "{}").statusCode()).isEqualTo(403);
        // Rota pública de estudante não ganha gabarito, estado nem fingerprint.
        var receipt = imported(cookie, csrf(cookie), QuestionPurpose.SIMULADO, "security.public");
        var id = UUID.fromString(receipt.path("results").get(0).path("questionId").asString());
        assertThat(send("GET", "/api/v1/question-bank/questions/" + id, null, null, null).statusCode()).isEqualTo(404);
        decide(id, EditorialDecision.Action.SUBMIT, null); decide(id, EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.AI_ONLY);
        var published = send("GET", "/api/v1/question-bank/questions/" + id, null, null, null);
        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.body()).doesNotContain("correctAlternativeId", "fingerprint", "reviewKind", "AI_ONLY", "Resolução sintética");
    }

    @Test void b_searchDetailAndReviewKindFilterWithoutWrites() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.PRATICA_AULA, "admin.search-ai", "admin.search-human", "admin.search-draft", "admin.search-legacy");
        var ids = new HashMap<String, UUID>();
        for (var r : receipt.path("results")) ids.put(r.path("origin").path("key").asString(), UUID.fromString(r.path("questionId").asString()));
        decide(ids.get("admin.search-ai"), EditorialDecision.Action.SUBMIT, null);
        decide(ids.get("admin.search-ai"), EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.AI_ONLY);
        decide(ids.get("admin.search-human"), EditorialDecision.Action.SUBMIT, null);
        decide(ids.get("admin.search-human"), EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.HUMAN_PEER);
        // Aprovação histórica (anterior à V16) sem origem: gravada diretamente como o legado faria.
        var legacy = ids.get("admin.search-legacy");
        var revision = jdbc.queryForObject("select id from qb_revision where question_id=?", UUID.class, legacy);
        var fingerprint = jdbc.queryForObject("select fingerprint from qb_revision where id=?", String.class, revision);
        new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource())).executeWithoutResult(t -> {
            for (var row : new Object[][]{{0, "SUBMIT"}, {1, "APPROVE"}})
                jdbc.update("insert into qb_decision(id,question_id,sequence,revision_id,action,fingerprint,responsible,decided_at,checklist,rationale) values (?,?,?,?,?,?,?,now(),?,?)",
                        UUID.randomUUID(), legacy, row[0], revision, row[1], fingerprint, "Legado sintético", "ACCESSIBILITY,CLARITY,CLASSIFICATION,CORRECTNESS,DISTRACTORS,SOURCES,STEP_BY_STEP", "Legado sintético");
            jdbc.update("update qb_revision set status='APPROVED' where id=?", revision);
            jdbc.update("update qb_question set eligible_revision_id=? where id=?", revision, legacy);
        });
        int before = writes();
        var all = ok(send("GET", BASE + "?keyPrefix=admin.search-&purpose=PRATICA_AULA&size=10", cookie, null, null));
        assertThat(all.get("total").asInt()).isEqualTo(4);
        assertThat(all.get("items")).extracting(i -> i.path("origin").path("key").asString())
                .containsExactly("admin.search-ai", "admin.search-draft", "admin.search-human", "admin.search-legacy");
        var ai = ok(send("GET", BASE + "?keyPrefix=admin.search-&reviewKind=AI_ONLY", cookie, null, null));
        assertThat(ai.get("items")).extracting(i -> i.path("origin").path("key").asString()).containsExactly("admin.search-ai");
        assertThat(ai.get("items").get(0).path("eligible").path("reviewKind").asString()).isEqualTo("AI_ONLY");
        var human = ok(send("GET", BASE + "?keyPrefix=admin.search-&reviewKind=HUMAN_PEER", cookie, null, null));
        assertThat(human.get("items")).extracting(i -> i.path("origin").path("key").asString()).containsExactly("admin.search-human");
        var eligible = ok(send("GET", BASE + "?keyPrefix=admin.search-&hasEligible=true", cookie, null, null));
        assertThat(eligible.get("total").asInt()).isEqualTo(3);
        var legacyItem = eligible.get("items").get(2);
        assertThat(legacyItem.path("origin").path("key").asString()).isEqualTo("admin.search-legacy");
        assertThat(legacyItem.path("eligible").path("reviewKind").isNull()).isTrue(); // não informado, nunca inferido
        var drafts = ok(send("GET", BASE + "?keyPrefix=admin.search-&latestStatus=DRAFT&disciplineId=portugues&topicId=P01", cookie, null, null));
        assertThat(drafts.get("items")).extracting(i -> i.path("origin").path("key").asString()).containsExactly("admin.search-draft");
        assertThat(ok(send("GET", BASE + "?keyPrefix=admin.search-&purpose=SIMULADO", cookie, null, null)).get("total").asInt()).isZero();
        var page = ok(send("GET", BASE + "?keyPrefix=admin.search-&size=3&page=1", cookie, null, null));
        assertThat(page.get("total").asInt()).isEqualTo(4); assertThat(page.get("items").size()).isEqualTo(1);
        var future = ok(send("GET", BASE + "?keyPrefix=admin.search-&activityFrom=2999-01-01T00:00:00Z", cookie, null, null));
        assertThat(future.get("total").asInt()).isZero();
        for (var bad : List.of("?size=0", "?size=101", "?page=-1", "?topicId=X99", "?reviewKind=HUMAN", "?latestStatus=GONE",
                "?activityFrom=2026-02-01T00:00:00Z&activityTo=2026-01-01T00:00:00Z", "?hasEligible=talvez")) {
            var response = send("GET", BASE + bad, cookie, null, null);
            assertThat(response.statusCode()).as(bad).isEqualTo(400);
            assertThat(json.readTree(response.body()).get("error").asString()).isEqualTo("INVALID_QUERY");
        }
        var detail = ok(send("GET", BASE + "/" + ids.get("admin.search-ai"), cookie, null, null));
        assertThat(detail.path("purpose").asString()).isEqualTo("PRATICA_AULA");
        assertThat(detail.path("revisions").get(0).path("status").asString()).isEqualTo("APPROVED");
        assertThat(detail.path("revisions").get(0).path("content").path("resolution").path("text").asString()).contains("Resolução sintética");
        assertThat(detail.path("revisions").get(0).path("content").has("correctAlternativeId")).isTrue();
        assertThat(detail.path("decisions")).extracting(d -> d.path("action").asString() + ":" + d.path("reviewKind").asString(""))
                .containsExactly("SUBMIT:", "APPROVE:AI_ONLY");
        assertThat(detail.path("imports").get(0).path("receiptId").asString()).isEqualTo(receipt.path("confirmation").path("nonce").asString());
        assertThat(detail.path("eligibleRevisionId").asString()).isEqualTo(detail.path("revisions").get(0).path("id").asString());
        var missing = send("GET", BASE + "/" + UUID.randomUUID(), cookie, null, null);
        assertThat(missing.statusCode()).isEqualTo(404); assertThat(missing.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(send("GET", BASE + "/not-a-uuid", cookie, null, null).statusCode()).isEqualTo(403); // rota fora da lista explícita
        assertThat(writes()).isEqualTo(before);
    }

    @Test void c_lookupClassifiesLocalPackageWithoutReserving() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "lookup.same", "lookup.eligible");
        var doc = document(QuestionPurpose.SIMULADO, "lookup.same", "lookup.eligible");
        var fingerprints = new HashMap<String, String>();
        for (var item : doc.items()) fingerprints.put(item.origin().key(), item.fingerprint());
        var eligibleId = UUID.fromString(receipt.path("results").get(0).path("origin").path("key").asString().equals("lookup.eligible")
                ? receipt.path("results").get(0).path("questionId").asString() : receipt.path("results").get(1).path("questionId").asString());
        decide(eligibleId, EditorialDecision.Action.SUBMIT, null); decide(eligibleId, EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.HUMAN_PEER);
        // Nova revisão em rascunho: elegível continua sendo a primeira.
        questions.transaction(s -> { var q = s.loadForUpdate(eligibleId).orElseThrow(); q.append(UUID.randomUUID(), QuestionFixtures.content("Correção sintética lookup")); s.save(q); return null; });
        int before = writes();
        var body = "{\"items\":["
                + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.same\"},\"fingerprint\":\"" + fingerprints.get("lookup.same") + "\"},"
                + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.eligible\"},\"fingerprint\":\"" + fingerprints.get("lookup.eligible") + "\"},"
                + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.other\"},\"fingerprint\":\"" + "a".repeat(64) + "\"},"
                + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.presence\"},\"fingerprint\":null},"
                + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.same-different\"},\"fingerprint\":\"" + "b".repeat(64) + "\"}]}";
        var result = ok(send("POST", BASE + "/lookup", cookie, csrf, body)).get("items");
        assertThat(result).extracting(i -> i.path("origin").path("key").asString() + ":" + i.path("match").asString())
                .containsExactly("lookup.same:SAME_AS_LATEST", "lookup.eligible:SAME_AS_ELIGIBLE", "lookup.other:ABSENT", "lookup.presence:ABSENT", "lookup.same-different:ABSENT");
        assertThat(result.get(1).path("matchingRevisionId").asString()).isEqualTo(result.get(1).path("eligibleRevisionId").asString());
        assertThat(result.get(1).path("latestStatus").asString()).isEqualTo("DRAFT");
        var present = ok(send("POST", BASE + "/lookup", cookie, csrf,
                "{\"items\":[{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.same\"},\"fingerprint\":null},"
                        + "{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"lookup.eligible\"},\"fingerprint\":\"" + "c".repeat(64) + "\"}]}")).get("items");
        assertThat(present).extracting(i -> i.path("match").asString()).containsExactly("PRESENT", "DIFFERENT");
        assertThat(writes()).isEqualTo(before);
        for (var bad : List.of("{\"items\":[]}", "{\"items\":null}", "{\"items\":[{\"origin\":{\"namespace\":\"x\",\"key\":\"k\"},\"fingerprint\":\"XYZ\"}]}",
                "{\"items\":[{\"origin\":{\"namespace\":\"x\",\"key\":\"k\"},\"fingerprint\":null},{\"origin\":{\"namespace\":\"x\",\"key\":\"k\"},\"fingerprint\":null}]}")) {
            assertThat(send("POST", BASE + "/lookup", cookie, csrf, bad).statusCode()).as(bad).isEqualTo(400);
        }
        for (var malformed : List.of("{\"items\":[],\"items\":[]}", "{\"items\":[]} {}", "{\"items\":[],\"extra\":1}", "{broken")) {
            var response = send("POST", BASE + "/lookup", cookie, csrf, malformed);
            assertThat(response.statusCode()).as(malformed).isEqualTo(400);
            assertThat(json.readTree(response.body()).get("error").asString()).isEqualTo("INVALID_REQUEST");
        }
        var tooMany = new StringBuilder("{\"items\":[");
        for (int i = 0; i <= 1000; i++) tooMany.append(i == 0 ? "" : ",").append("{\"origin\":{\"namespace\":\"n\",\"key\":\"k").append(i).append("\"},\"fingerprint\":null}");
        assertThat(send("POST", BASE + "/lookup", cookie, csrf, tooMany.append("]}").toString()).statusCode()).isEqualTo(400);
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + BASE + "/lookup")).header("Cookie", cookie).header("X-CSRF-TOKEN", csrf)
                .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertThat(http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(415);
    }

    @Test void d_exportIsReimportableWithIdenticalFingerprints() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.PRATICA_AULA, "admin.export-a", "admin.export-b");
        var nonce = receipt.path("confirmation").path("nonce").asString();
        var original = document(QuestionPurpose.PRATICA_AULA, "admin.export-a", "admin.export-b");
        int before = writes();
        var byReceipt = ok(send("POST", BASE + "/export", cookie, csrf, "{\"importReceiptId\":\"" + nonce + "\"}"));
        var exported = json.treeToValue(byReceipt.get("package"), QuestionImportReader.Document.class);
        var keys = List.of("admin.export-a", "admin.export-b");
        var pack = reader.read(exported, keys);
        assertThat(pack.digest()).isEqualTo(byReceipt.get("packageDigest").asString());
        assertThat(pack.digest()).isEqualTo(reader.read(original, keys).digest()); // mesmo pacote canônico
        assertThat(exported.items()).extracting(QuestionImportReader.Item::fingerprint)
                .containsExactlyElementsOf(original.items().stream().map(QuestionImportReader.Item::fingerprint).toList());
        var ids = new ArrayList<String>(); for (var r : receipt.path("results")) ids.add("\"" + r.path("questionId").asString() + "\"");
        var byIds = ok(send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[" + String.join(",", ids) + "]}"));
        assertThat(byIds.get("packageDigest").asString()).isEqualTo(byReceipt.get("packageDigest").asString());
        // Reimportar o pacote exportado reconhece conteúdo idêntico, sem novas revisões.
        var preview = ok(send("POST", "/api/v1/admin/question-imports/preview", cookie, csrf, json.writeValueAsString(new PreviewRequest(exported, keys))));
        assertThat(preview.get("items")).extracting(i -> i.path("change").asString()).containsOnly("IDENTICAL");
        assertThat(writes()).isEqualTo(before);
        // Recusas explícitas e sanitizadas.
        var other = imported(cookie, csrf, QuestionPurpose.SIMULADO, "export-simulado");
        var mixed = send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[" + ids.get(0) + ",\"" + other.path("results").get(0).path("questionId").asString() + "\"]}");
        assertThat(mixed.statusCode()).isEqualTo(422); assertThat(json.readTree(mixed.body()).get("reason").asString()).isEqualTo("MIXED_PURPOSE");
        var unknown = send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[\"" + UUID.randomUUID() + "\"]}");
        assertThat(unknown.statusCode()).isEqualTo(422); assertThat(json.readTree(unknown.body()).get("reason").asString()).isEqualTo("NOT_FOUND");
        var unknownReceipt = send("POST", BASE + "/export", cookie, csrf, "{\"importReceiptId\":\"" + UUID.randomUUID() + "\"}");
        assertThat(json.readTree(unknownReceipt.body()).get("reason").asString()).isEqualTo("NOT_FOUND");
        // Questão fora do formato administrativo (origem local) não é exportável.
        var local = Question.draft(UUID.randomUUID(), new QuestionOrigin("local-cli-v1", "P01-Q01"), UUID.randomUUID(), QuestionFixtures.content("Origem local"));
        questions.transaction(s -> { s.insert(local); return null; });
        var notExportable = send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[\"" + local.id() + "\"]}");
        assertThat(json.readTree(notExportable.body()).get("reason").asString()).isEqualTo("ORIGIN_NOT_EXPORTABLE");
        for (var bad : List.of("{}", "{\"questionIds\":[]}", "{\"questionIds\":[" + ids.get(0) + "],\"importReceiptId\":\"" + nonce + "\"}",
                "{\"questionIds\":[" + ids.get(0) + "," + ids.get(0) + "]}"))
            assertThat(send("POST", BASE + "/export", cookie, csrf, bad).statusCode()).as(bad).isEqualTo(400);
    }

    @SuppressWarnings("unchecked")
    static void contract(String schema, JsonNode body) throws Exception {
        Map<String, Object> api = new org.yaml.snakeyaml.Yaml().load(java.nio.file.Files.readString(java.nio.file.Path.of("openapi.yaml")));
        var schemas = (Map<String, Object>) ((Map<String, Object>) api.get("components")).get("schemas");
        var definition = (Map<String, Object>) schemas.get(schema);
        assertThat(definition.get("additionalProperties")).as(schema).isEqualTo(false);
        var names = new ArrayList<String>(); body.propertyNames().forEach(names::add);
        assertThat(names).as(schema).containsExactlyInAnyOrderElementsOf((List<String>) definition.get("required"));
    }

    @Test void e_responsesFollowDocumentedOpenApiContracts() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "contract.one");
        var id = receipt.path("results").get(0).path("questionId").asString();
        var page = ok(send("GET", BASE + "?keyPrefix=contract.", cookie, null, null));
        contract("AdminQuestionPage", page); contract("AdminQuestionSummary", page.get("items").get(0));
        contract("AdminLatestRevision", page.get("items").get(0).get("latest"));
        var detail = ok(send("GET", BASE + "/" + id, cookie, null, null));
        contract("AdminQuestionDetail", detail); contract("AdminQuestionRevision", detail.get("revisions").get(0));
        contract("AdminImportLink", detail.get("imports").get(0));
        var lookup = ok(send("POST", BASE + "/lookup", cookie, csrf, "{\"items\":[{\"origin\":{\"namespace\":\"admin-question-v1\",\"key\":\"contract.one\"},\"fingerprint\":null}]}"));
        contract("AdminQuestionLookup", lookup); contract("AdminQuestionLookupResult", lookup.get("items").get(0));
        var export = ok(send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[\"" + id + "\"]}"));
        contract("AdminQuestionExport", export);
        decide(UUID.fromString(id), EditorialDecision.Action.SUBMIT, null);
        contract("AdminEditorialDecision", ok(send("GET", BASE + "/" + id, cookie, null, null)).get("decisions").get(0));
        var refused = send("POST", BASE + "/export", cookie, csrf, "{\"questionIds\":[\"" + UUID.randomUUID() + "\"]}");
        contract("AdminQuestionExportError", json.readTree(refused.body()));
        contract("QuestionImportError", json.readTree(send("GET", BASE + "?size=0", cookie, null, null).body()));
    }
}

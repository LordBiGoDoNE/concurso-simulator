package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.domain.*;
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
import tools.jackson.databind.node.ObjectNode;
import static br.com.concursosimulator.questionbank.AdministrativeImportFixtures.document;
import static org.assertj.core.api.Assertions.*;

/** Decisões editoriais em lote (#177) pela cadeia HTTP/segurança real e PostgreSQL isolado; dados sintéticos. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local") @Testcontainers
class EditorialDecisionBatchHttpTests {
    static final UUID OPERATOR = UUID.fromString("99999999-1111-2222-3333-444444444444");
    static final String BASE = "/api/v1/admin/editorial-decisions";
    static final List<String> ALL_CHECKS = List.of("ACCESSIBILITY", "CLARITY", "CLASSIFICATION", "CORRECTNESS", "DISTRACTORS", "SOURCES", "STEP_BY_STEP");
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl); r.add("spring.datasource.username", PG::getUsername); r.add("spring.datasource.password", PG::getPassword);
        r.add("app.question-import.destination-id", () -> "isolated-test");
        r.add("app.question-import.confirmation-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcIndexedSessionRepository sessions; @Autowired JdbcTemplate jdbc;
    @Autowired br.com.concursosimulator.questionbank.application.port.QuestionStore questions;
    final HttpClient http = HttpClient.newHttpClient(); final JsonMapper json = JsonMapper.builder().build();

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
    String admin() { return session(new UserPrincipal(OPERATOR), jdbc.queryForObject("select auth_version from admin_registration where id=1", Long.class)); }
    HttpResponse<String> send(String path, String cookie, String csrf, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) request.header("Cookie", cookie); if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    String csrf(String cookie) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/csrf")).header("Cookie", cookie).GET().build();
        return json.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body()).get("token").asString();
    }
    JsonNode ok(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return json.readTree(response.body());
    }
    JsonNode imported(String cookie, String csrf, QuestionPurpose purpose, String... keys) throws Exception {
        var doc = document(purpose, keys);
        var preview = ok(send("/api/v1/admin/question-imports/preview", cookie, csrf, json.writeValueAsString(new PreviewRequest(doc, List.of(keys)))));
        return ok(send("/api/v1/admin/question-imports/apply", cookie, csrf,
                json.writeValueAsString(new ApplyRequest(doc, List.of(keys), preview.get("confirmationToken").asString())))).path("receipt");
    }
    ObjectNode envelope(String action, String kind, List<String> checks, String rationale) {
        var node = json.createObjectNode().put("action", action);
        if (kind == null) node.putNull("reviewKind"); else node.put("reviewKind", kind);
        var array = node.putArray("checklist"); checks.forEach(array::add);
        node.put("rationale", rationale); return node;
    }
    ObjectNode withoutField(ObjectNode node, String field) { var copy = node.deepCopy(); copy.remove(field); return copy; }
    ObjectNode byReceipt(ObjectNode envelope, String nonce) { var n = envelope.deepCopy(); n.putNull("items"); n.put("importReceiptId", nonce); return n; }
    /** Corpo de aplicação a partir da prévia: mesmo envelope, estado esperado e token. */
    String applyBody(JsonNode preview) {
        var node = json.createObjectNode().put("action", preview.get("action").asString());
        node.set("reviewKind", preview.get("reviewKind")); node.set("checklist", preview.get("checklist"));
        node.put("rationale", preview.get("rationale").asString()); node.set("items", preview.get("expected"));
        node.set("confirmationToken", preview.get("confirmationToken")); return json.writeValueAsString(node);
    }
    int decisions() { return jdbc.queryForObject("select count(*) from qb_decision", Integer.class); }
    List<UUID> ids(JsonNode receipt) { var ids = new ArrayList<UUID>(); for (var r : receipt.path("results")) ids.add(UUID.fromString(r.path("questionId").asString())); return ids; }

    @Test void realSecurityChainRejectsVisitorUnprivilegedPendingAndMissingCsrfBeforeParsing() throws Exception {
        var visitor = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        var visitorCookie = visitor.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        assertThat(send(BASE + "/preview", visitorCookie, json.readTree(visitor.body()).get("token").asString(), "{broken").statusCode()).isEqualTo(401);
        assertThat(send(BASE + "/preview", null, null, "{broken").statusCode()).isEqualTo(403); // sem CSRF, antes de qualquer leitura
        var common = session(new UserPrincipal(UUID.randomUUID()), null);
        assertThat(send(BASE + "/preview", common, csrf(common), "{broken").statusCode()).isEqualTo(403);
        var cookie = admin();
        assertThat(send(BASE + "/preview", cookie, null, "{broken").statusCode()).isEqualTo(403);
        jdbc.update("update admin_registration set must_change=true where id=1");
        assertThat(send(BASE + "/apply", cookie, csrf(cookie), "{broken").statusCode()).isEqualTo(403);
        jdbc.update("update admin_registration set must_change=false where id=1");
        assertThat(send(BASE + "/withdraw", cookie, csrf(cookie), "{}").statusCode()).isEqualTo(403);
        var malformed = send(BASE + "/preview", cookie, csrf(cookie), "{broken");
        assertThat(malformed.statusCode()).isEqualTo(400); assertThat(malformed.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test void submitAndApproveWholeImportReceiptWithReviewKindReceiptAndReplay() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.PRATICA_AULA, "admin.batch-a", "admin.batch-b", "admin.batch-c");
        var nonce = receipt.path("confirmation").path("nonce").asString(); var ids = ids(receipt);
        int before = decisions();
        var submitPreview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Lote sintético de envio"), nonce))));
        assertThat(submitPreview.get("confirmable").asBoolean()).isTrue();
        assertThat(submitPreview.get("items")).extracting(i -> i.path("currentStatus").asString() + ">" + i.path("resultingStatus").asString()).containsOnly("DRAFT>IN_REVIEW");
        assertThat(decisions()).isEqualTo(before); // prévia sem efeitos
        var submitted = ok(send(BASE + "/apply", cookie, csrf, applyBody(submitPreview)));
        assertThat(submitted.path("receipt").path("decisions").size()).isEqualTo(3);
        assertThat(decisions()).isEqualTo(before + 3);

        var approvePreview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("APPROVE", "AI_ONLY", ALL_CHECKS, "Revisão somente por IA, fixture"), nonce))));
        assertThat(approvePreview.get("items")).extracting(i -> i.path("resultingStatus").asString()).containsOnly("APPROVED");
        var body = applyBody(approvePreview);
        var approved = ok(send(BASE + "/apply", cookie, csrf, body));
        assertThat(approved.get("replayed").asBoolean()).isFalse();
        var batch = approved.path("receipt");
        assertThat(batch.path("reviewKind").asString()).isEqualTo("AI_ONLY");
        assertThat(batch.path("responsible").asString()).isEqualTo("admin:" + OPERATOR);
        for (var id : ids) {
            assertThat(jdbc.queryForObject("select eligible_revision_id is not null from qb_question where id=?", Boolean.class, id)).isTrue();
            var last = jdbc.queryForMap("select action,review_kind,responsible,rationale from qb_decision where question_id=? order by sequence desc limit 1", id);
            assertThat(last).containsEntry("action", "APPROVE").containsEntry("review_kind", "AI_ONLY").containsEntry("responsible", "admin:" + OPERATOR);
            assertThat(last.get("rationale").toString()).startsWith("Revisão somente por IA, fixture [lote ").contains(batch.path("confirmation").path("nonce").asString());
        }
        // Resposta perdida: o mesmo token devolve o recibo original, sem decisões duplicadas.
        var replay = ok(send(BASE + "/apply", cookie, csrf, body));
        assertThat(replay.get("replayed").asBoolean()).isTrue();
        assertThat(replay.path("receipt")).isEqualTo(batch);
        assertThat(decisions()).isEqualTo(before + 6);
        assertThat(jdbc.queryForObject("select count(*) from qb_decision_batch", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from qb_decision_batch_item where nonce=?", Integer.class,
                UUID.fromString(batch.path("confirmation").path("nonce").asString()))).isEqualTo(3);
        // Origem da revisão consultável na consulta administrativa.
        var cookieGet = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/admin/questions?keyPrefix=admin.batch-&reviewKind=AI_ONLY")).header("Cookie", cookie).GET().build();
        assertThat(json.readTree(http.send(cookieGet, HttpResponse.BodyHandlers.ofString()).body()).get("total").asInt()).isEqualTo(3);
    }

    @Test void divergenceAfterPreviewRejectsWholeBatchAndListsItems() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "batch.diverge-a", "batch.diverge-b");
        var ids = ids(receipt); var nonce = receipt.path("confirmation").path("nonce").asString();
        var preview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Envio sintético"), nonce))));
        // Operador concorrente decide uma das questões entre a prévia e a aplicação.
        var changed = ids.getFirst();
        questions.transaction(s -> { var q = s.loadForUpdate(changed).orElseThrow();
            q.decide(new EditorialDecision(EditorialDecision.Action.SUBMIT, q.id(), q.latest().id(), q.latest().fingerprint(), "Concorrente sintético",
                    java.time.Instant.now(), Set.of(), "Mudança concorrente", null)); s.save(q); return null; });
        int before = decisions();
        var diverged = send(BASE + "/apply", cookie, csrf, applyBody(preview));
        assertThat(diverged.statusCode()).isEqualTo(409);
        var error = json.readTree(diverged.body());
        assertThat(error.get("error").asString()).isEqualTo("BATCH_DIVERGED");
        assertThat(error.get("questionIds")).extracting(JsonNode::asString).containsExactly(changed.toString());
        assertThat(decisions()).isEqualTo(before); // tudo-ou-nada: a outra questão também não mudou
        assertThat(jdbc.queryForObject("select status from qb_revision where question_id=?", String.class, ids.get(1))).isEqualTo("DRAFT");
        // Nova prévia mostra o estado atual: a questão já enviada não é confirmável.
        var again = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Envio sintético"), nonce))));
        assertThat(again.get("confirmable").asBoolean()).isFalse(); assertThat(again.get("confirmationToken").isNull()).isTrue();
        assertThat(again.get("items")).extracting(i -> i.path("refusal").asString("")).contains("INVALID_TRANSITION", "");
    }

    @Test void failureInsideTheUnitRollsBackEveryDecision() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "batch.rollback-a", "batch.rollback-b");
        var nonce = receipt.path("confirmation").path("nonce").asString();
        var preview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Envio sintético"), nonce))));
        jdbc.execute("create function fail_batch_item() returns trigger language plpgsql as $$ begin raise exception 'falha sintética'; end $$");
        jdbc.execute("create trigger fail_batch_item before insert on qb_decision_batch_item for each row execute function fail_batch_item()");
        int before = decisions();
        try {
            var failed = send(BASE + "/apply", cookie, csrf, applyBody(preview));
            assertThat(failed.statusCode()).isEqualTo(500);
            assertThat(failed.body()).doesNotContain("falha sintética", "qb_decision");
        } finally {
            jdbc.execute("drop trigger fail_batch_item on qb_decision_batch_item"); jdbc.execute("drop function fail_batch_item()");
        }
        assertThat(decisions()).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from qb_revision r join qb_import_revision i on i.revision_id=r.id where i.nonce=? and r.status<>'DRAFT'",
                Integer.class, UUID.fromString(nonce))).isZero();
        // O mesmo token continua aplicável depois da falha (nada foi gravado).
        assertThat(ok(send(BASE + "/apply", cookie, csrf, applyBody(preview))).get("replayed").asBoolean()).isFalse();
    }

    @Test void previewRefusalsAndTamperedConfirmationNeverWrite() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "batch.refuse-a");
        var nonce = receipt.path("confirmation").path("nonce").asString();
        var item = receipt.path("results").get(0);
        int before = decisions();
        var draftApproval = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("APPROVE", "HUMAN_PEER", ALL_CHECKS, "Aprovação sintética"), nonce))));
        assertThat(draftApproval.get("confirmable").asBoolean()).isFalse(); assertThat(draftApproval.get("confirmationToken").isNull()).isTrue();
        assertThat(draftApproval.get("items").get(0).path("refusal").asString()).isEqualTo("INVALID_TRANSITION");
        var incomplete = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("APPROVE", "HUMAN_PEER", List.of("CORRECTNESS"), "Aprovação sintética"), nonce))));
        assertThat(incomplete.get("items").get(0).path("refusal").asString()).isEqualTo("CHECKLIST_INCOMPLETE");
        var explicit = envelope("SUBMIT", null, List.of(), "Envio sintético");
        explicit.putArray("items").addObject().put("questionId", item.path("questionId").asString()).put("revisionId", item.path("revisionId").asString()).put("fingerprint", "f".repeat(64));
        explicit.putNull("importReceiptId");
        assertThat(ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(explicit))).get("items").get(0).path("refusal").asString()).isEqualTo("FINGERPRINT_MISMATCH");
        for (var invalid : List.of(byReceipt(envelope("APPROVE", null, ALL_CHECKS, "Sem origem"), nonce), byReceipt(envelope("WITHDRAW", null, List.of(), "Fora da entrega"), nonce),
                byReceipt(envelope("RETURN_TO_DRAFT", null, List.of(), "Fora da entrega"), nonce), byReceipt(envelope("SUBMIT", null, List.of(), " "), nonce),
                byReceipt(envelope("SUBMIT", null, List.of("CLARITY", "CLARITY"), "Repetido"), nonce), byReceipt(envelope("SUBMIT", null, List.of(), "x"), UUID.randomUUID().toString()))) {
            var response = send(BASE + "/preview", cookie, csrf, json.writeValueAsString(invalid));
            assertThat(response.statusCode()).as(invalid.toString()).isEqualTo(400);
            assertThat(json.readTree(response.body()).get("error").asString()).isEqualTo("INVALID_BATCH");
        }
        // Contrato: reviewKind é opcional na requisição (ausente = null); APPROVE sem origem é recusado pelo domínio.
        var submitWithout = byReceipt(envelope("SUBMIT", null, List.of(), "Envio sem origem declarada"), nonce); submitWithout.remove("reviewKind");
        var submitted = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(submitWithout)));
        assertThat(submitted.get("confirmable").asBoolean()).isTrue(); assertThat(submitted.get("reviewKind").isNull()).isTrue();
        // editorialSequence ausente no apply é corpo inválido (400), não divergência de estado (409).
        var noSequence = (ObjectNode) json.readTree(applyBody(submitted));
        ((ObjectNode) noSequence.get("items").get(0)).remove("editorialSequence");
        var refused = send(BASE + "/apply", cookie, csrf, json.writeValueAsString(noSequence));
        assertThat(refused.statusCode()).as(refused.body()).isEqualTo(400);
        assertThat(json.readTree(refused.body()).get("error").asString()).isEqualTo("INVALID_BATCH");
        assertThat(decisions()).isEqualTo(before);
        var approveWithout = byReceipt(envelope("APPROVE", null, ALL_CHECKS, "Sem origem"), nonce); approveWithout.remove("reviewKind");
        for (var missing : List.of(approveWithout, withoutField(submitWithout, "checklist"), withoutField(submitWithout, "rationale"), withoutField(submitWithout, "action"))) {
            var response = send(BASE + "/preview", cookie, csrf, json.writeValueAsString(missing));
            assertThat(response.statusCode()).as(missing.toString()).isEqualTo(400);
        }
        var both = byReceipt(envelope("SUBMIT", null, List.of(), "Ambos"), nonce); both.putArray("items");
        assertThat(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(both)).statusCode()).isEqualTo(400);
        var tooMany = envelope("SUBMIT", null, List.of(), "Excesso"); var array = tooMany.putArray("items"); tooMany.putNull("importReceiptId");
        for (int i = 0; i <= 100; i++) array.addObject().put("questionId", UUID.randomUUID().toString()).put("revisionId", UUID.randomUUID().toString()).put("fingerprint", "a".repeat(64));
        assertThat(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(tooMany)).statusCode()).isEqualTo(400);
        var unknown = envelope("SUBMIT", null, List.of(), "Desconhecida"); unknown.putNull("importReceiptId");
        unknown.putArray("items").addObject().put("questionId", UUID.randomUUID().toString()).put("revisionId", UUID.randomUUID().toString()).put("fingerprint", "a".repeat(64));
        assertThat(ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(unknown))).get("items").get(0).path("refusal").asString()).isEqualTo("QUESTION_NOT_FOUND");

        var preview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Envio sintético"), nonce))));
        var tampered = (ObjectNode) json.readTree(applyBody(preview)); tampered.put("rationale", "Outra justificativa");
        assertMismatch(send(BASE + "/apply", cookie, csrf, json.writeValueAsString(tampered)));
        var alteredState = (ObjectNode) json.readTree(applyBody(preview)); ((ObjectNode) alteredState.get("items").get(0)).put("editorialSequence", 7);
        assertMismatch(send(BASE + "/apply", cookie, csrf, json.writeValueAsString(alteredState)));
        var badToken = (ObjectNode) json.readTree(applyBody(preview)); badToken.put("confirmationToken", "X" + preview.get("confirmationToken").asString());
        assertMismatch(send(BASE + "/apply", cookie, csrf, json.writeValueAsString(badToken)));
        // Token de importação não confirma lote de decisões (separação de domínio).
        var importPreview = ok(send("/api/v1/admin/question-imports/preview", cookie, csrf,
                json.writeValueAsString(new PreviewRequest(document(QuestionPurpose.SIMULADO, "batch.other"), List.of("batch.other")))));
        var crossToken = (ObjectNode) json.readTree(applyBody(preview)); crossToken.put("confirmationToken", importPreview.get("confirmationToken").asString());
        assertMismatch(send(BASE + "/apply", cookie, csrf, json.writeValueAsString(crossToken)));
        // Outro ADM/ator não reutiliza o token (sessão comum sequer chega ao controller).
        assertThat(decisions()).isEqualTo(before);
        for (var malformed : List.of("{\"action\":\"SUBMIT\",\"action\":\"SUBMIT\"}", applyBody(preview) + " {}", applyBody(preview).replace("\"rationale\"", "\"extra\":1,\"rationale\""))) {
            var response = send(BASE + "/apply", cookie, csrf, malformed);
            assertThat(response.statusCode()).as(malformed).isEqualTo(400);
        }
        assertThat(decisions()).isEqualTo(before);
    }
    void assertMismatch(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
        assertThat(json.readTree(response.body()).get("error").asString()).isEqualTo("CONFIRMATION_MISMATCH");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test void concurrentReplayOfTheSameTokenReturnsTheReceiptInsteadOfDivergence() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "batch.concurrent-a", "batch.concurrent-b");
        var preview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(byReceipt(envelope("SUBMIT", null, List.of(), "Envio concorrente"), receipt.path("confirmation").path("nonce").asString()))));
        var body = applyBody(preview); int before = decisions();
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> send(BASE + "/apply", cookie, csrf, body)); var second = pool.submit(() -> send(BASE + "/apply", cookie, csrf, body));
            var responses = List.of(first.get(), second.get());
            assertThat(responses).allSatisfy(r -> assertThat(r.statusCode()).as(r.body()).isEqualTo(200));
            assertThat(responses).extracting(r -> json.readTree(r.body()).get("replayed").asBoolean()).containsExactlyInAnyOrder(false, true);
            assertThat(json.readTree(responses.get(0).body()).path("receipt")).isEqualTo(json.readTree(responses.get(1).body()).path("receipt"));
        } finally { pool.shutdownNow(); }
        assertThat(decisions()).isEqualTo(before + 2);
    }

    @Test void explicitSelectionAppliesAndResponsesFollowOpenApiAndBatchTokenIsNotAnImportToken() throws Exception {
        var cookie = admin(); var csrf = csrf(cookie);
        var receipt = imported(cookie, csrf, QuestionPurpose.SIMULADO, "batch.explicit");
        var item = receipt.path("results").get(0);
        var fingerprint = jdbc.queryForObject("select fingerprint from qb_revision where id=?", String.class, UUID.fromString(item.path("revisionId").asString()));
        var explicit = envelope("SUBMIT", null, List.of(), "Seleção explícita"); explicit.putNull("importReceiptId");
        explicit.putArray("items").addObject().put("questionId", item.path("questionId").asString()).put("revisionId", item.path("revisionId").asString()).put("fingerprint", fingerprint);
        var preview = ok(send(BASE + "/preview", cookie, csrf, json.writeValueAsString(explicit)));
        AdministrativeQuestionHttpTests.contract("EditorialBatchPreview", preview);
        AdministrativeQuestionHttpTests.contract("EditorialBatchPreviewItem", preview.get("items").get(0));
        AdministrativeQuestionHttpTests.contract("EditorialBatchExpected", preview.get("expected").get(0));
        // Token do lote não confirma importação (separação de domínio nos dois sentidos).
        var doc = document(QuestionPurpose.SIMULADO, "batch.cross");
        var importApply = send("/api/v1/admin/question-imports/apply", cookie, csrf, json.writeValueAsString(new ApplyRequest(doc, List.of("batch.cross"), preview.get("confirmationToken").asString())));
        assertThat(importApply.statusCode()).isEqualTo(409);
        var applied = ok(send(BASE + "/apply", cookie, csrf, applyBody(preview)));
        AdministrativeQuestionHttpTests.contract("EditorialBatchApplied", applied);
        AdministrativeQuestionHttpTests.contract("EditorialBatchReceipt", applied.get("receipt"));
        var conflict = send(BASE + "/apply", cookie, csrf, applyBody(preview).replace("Seleção explícita", "Outra"));
        AdministrativeQuestionHttpTests.contract("EditorialBatchConflict", json.readTree(conflict.body()));
        assertThat(jdbc.queryForObject("select status from qb_revision where id=?", String.class, UUID.fromString(item.path("revisionId").asString()))).isEqualTo("IN_REVIEW");
    }
}

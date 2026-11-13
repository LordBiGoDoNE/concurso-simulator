package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries;
import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries.*;
import br.com.concursosimulator.questionbank.application.port.AdministrativeQuestionCatalog;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import br.com.concursosimulator.questionbank.domain.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Consulta administrativa somente leitura (#177). Autenticação, permissão ADM persistida, CSRF e
 * no-store pertencem à cadeia de segurança; aqui só há tradução HTTP para os casos de uso.
 */
@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdministrativeQuestionController {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AdministrativeQuestionController.class);
    private final AdministrativeQuestionQueries queries;
    public AdministrativeQuestionController(AdministrativeQuestionQueries queries) { this.queries = queries; }

    public record SummaryItem(UUID questionId, QuestionOrigin origin, QuestionPurpose purpose, int editorialSequence,
                              AdministrativeQuestionCatalog.LatestRevision latest, AdministrativeQuestionCatalog.EligibleRevision eligible,
                              Instant lastActivityAt) {}
    public record SearchResponse(int page, int size, long total, List<SummaryItem> items) {}
    public record RevisionView(UUID id, int number, Question.Status status, String fingerprint, QuestionImportReader.Content content) {}
    public record DecisionView(int sequence, EditorialDecision.Action action, UUID revisionId, String fingerprint, String responsible,
                               Instant at, List<EditorialDecision.Check> checklist, String rationale, EditorialDecision.ReviewKind reviewKind) {}
    public record DetailResponse(UUID questionId, QuestionOrigin origin, QuestionPurpose purpose, int editorialSequence, UUID eligibleRevisionId,
                                 List<RevisionView> revisions, List<DecisionView> decisions, List<AdministrativeQuestionCatalog.ImportLink> imports) {}
    public record LookupItem(QuestionOrigin origin, String fingerprint) {}
    public record LookupRequest(List<LookupItem> items) {}
    public record LookupResponse(List<LookupResult> items) {}
    public record ExportRequest(List<UUID> questionIds, UUID importReceiptId) {}
    public record ExportResponse(String packageDigest, String selectionDigest, List<String> keys,
                                 @JsonProperty("package") QuestionImportReader.Document document) {}
    /** Ausência é contrato explícito (404), não lista vazia nem 500. */
    public static final class QuestionNotFound extends RuntimeException {
        public QuestionNotFound() { super("QUESTION_NOT_FOUND"); }
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam(required = false) QuestionPurpose purpose,
            @RequestParam(required = false) String namespace, @RequestParam(required = false) String keyPrefix,
            @RequestParam(required = false) String disciplineId, @RequestParam(required = false) String topicId,
            @RequestParam(required = false) Question.Status latestStatus, @RequestParam(required = false) Boolean hasEligible,
            @RequestParam(required = false) EditorialDecision.ReviewKind reviewKind,
            @RequestParam(required = false) Instant activityFrom, @RequestParam(required = false) Instant activityTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal actor) {
        var criteria = new Criteria(purpose, namespace, keyPrefix, disciplineId, topicId, latestStatus, hasEligible, reviewKind, activityFrom, activityTo, page, size);
        var result = queries.search(criteria);
        LOG.info("Admin question search actor={} page={} size={} total={}", actor.id(), page, size, result.total());
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(new SearchResponse(page, size, result.total(),
                result.items().stream().map(s -> new SummaryItem(s.questionId(), s.origin(), s.origin().purpose(), s.editorialSequence(),
                        s.latest(), s.eligible(), s.lastActivityAt())).toList()));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<DetailResponse> detail(@PathVariable UUID questionId, @AuthenticationPrincipal UserPrincipal actor) {
        var detail = queries.detail(questionId).orElseThrow(QuestionNotFound::new);
        var q = detail.question();
        LOG.info("Admin question detail actor={} question={}", actor.id(), q.id());
        var revisions = q.revisions().stream().map(r -> new RevisionView(r.id(), r.number(), q.status(r.id()), r.fingerprint(),
                AdministrativeQuestionQueries.packageContent(r.content()))).toList();
        var decisions = new ArrayList<DecisionView>();
        for (int i = 0; i < q.decisions().size(); i++) {
            var d = q.decisions().get(i);
            decisions.add(new DecisionView(i, d.action(), d.revisionId(), d.fingerprint(), d.responsible(), d.at(),
                    d.checklist().stream().sorted().toList(), d.rationale(), d.reviewKind()));
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(new DetailResponse(q.id(), q.origin(), q.origin().purpose(),
                q.editorialSequence(), q.eligible().map(QuestionRevision::id).orElse(null), revisions, List.copyOf(decisions), detail.imports()));
    }

    @PostMapping(value = "/lookup", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LookupResponse> lookup(@RequestBody LookupRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        if (request.items() == null) throw new InvalidQuery();
        var keys = request.items().stream().map(i -> i == null ? null : new LookupKey(i.origin(), i.fingerprint())).toList();
        var results = queries.lookup(keys);
        LOG.info("Admin question lookup actor={} items={}", actor.id(), results.size());
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(new LookupResponse(results));
    }

    @PostMapping(value = "/export", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExportResponse> export(@RequestBody ExportRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        var export = queries.export(new ExportSelection(request.questionIds(), request.importReceiptId()));
        LOG.info("Admin question export actor={} items={} packageDigest={}", actor.id(), export.keys().size(), export.packageDigest());
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new ExportResponse(export.packageDigest(), export.selectionDigest(), export.keys(), export.document()));
    }
}

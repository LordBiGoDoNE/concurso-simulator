package br.com.concursosimulator.questionbank.web;

import static br.com.concursosimulator.questionbank.web.AdministrativeContracts.*;

import br.com.concursosimulator.identity.application.UserPrincipal;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase.Envelope;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase.InvalidBatch;
import br.com.concursosimulator.questionbank.application.QuestionImportTarget;
import br.com.concursosimulator.questionbank.application.port.EditorialBatchConfirmations;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Decisões editoriais em lote do ADM (#177): prévia sem escrita e aplicação confirmada. Acesso,
 * CSRF e no-store pertencem à cadeia de segurança; transições, ao domínio; aqui só há tradução HTTP.
 */
@RestController
@RequestMapping(
        value = "/api/v1/admin/editorial-decisions",
        consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
public class EditorialDecisionBatchController {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(EditorialDecisionBatchController.class);
    private final EditorialBatchUseCase batches;
    private final EditorialBatchConfirmations tokens;
    private final QuestionImportTarget target;

    public EditorialDecisionBatchController(
            EditorialBatchUseCase batches, EditorialBatchConfirmations tokens, QuestionImportTarget target) {
        this.batches = batches;
        this.tokens = tokens;
        this.target = target;
    }

    public record PreviewRequest(
            Action action,
            ReviewKind reviewKind,
            List<Check> checklist,
            String rationale,
            List<Target> items,
            UUID importReceiptId) {}

    public record PreviewResponse(
            String destinationId,
            Action action,
            ReviewKind reviewKind,
            List<Check> checklist,
            String rationale,
            List<BatchItem> items,
            List<Expected> expected,
            String digest,
            boolean confirmable,
            String confirmationToken,
            Instant expiresAt) {}

    public record ApplyRequest(
            Action action,
            ReviewKind reviewKind,
            List<Check> checklist,
            String rationale,
            List<Expected> items,
            String confirmationToken) {}

    public record ApplyResponse(String destinationId, BatchReceipt receipt, boolean replayed) {}

    @PostMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(
            @RequestBody PreviewRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        var envelope = envelope(request.action(), request.reviewKind(), request.checklist(), request.rationale());
        var preview = batches.preview(envelope, targets(request.items()), request.importReceiptId());
        var issued = preview.confirmable() ? tokens.issue(actor.id(), target.id(), preview.digest()) : null;
        LOG.info(
                "Editorial batch preview actor={} destination={} action={} items={} confirmable={} digest={}",
                actor.id(),
                target.id(),
                envelope.action(),
                preview.items().size(),
                preview.confirmable(),
                preview.digest());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(new PreviewResponse(
                        target.id(),
                        map(envelope.action(), Action.class),
                        map(envelope.reviewKind(), ReviewKind.class),
                        sorted(envelope.checklist()),
                        envelope.rationale(),
                        list(preview.items(), AdministrativeContracts::batchItem),
                        list(preview.expected(), AdministrativeContracts::expected),
                        preview.digest(),
                        preview.confirmable(),
                        issued == null ? null : issued.token(),
                        issued == null ? null : issued.expiresAt()));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyResponse> apply(
            @RequestBody ApplyRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        var envelope = envelope(request.action(), request.reviewKind(), request.checklist(), request.rationale());
        var outcome = batches.apply(
                envelope,
                expected(request.items()),
                tokens.verify(request.confirmationToken()),
                actor.id(),
                target.id());
        LOG.info(
                "Editorial batch committed actor={} destination={} action={} nonce={} decisions={} replayed={}",
                actor.id(),
                target.id(),
                envelope.action(),
                outcome.receipt().confirmation().nonce(),
                outcome.receipt().decisions().size(),
                outcome.replayed());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(new ApplyResponse(target.id(), batchReceipt(outcome.receipt()), outcome.replayed()));
    }

    private static Envelope envelope(Action action, ReviewKind kind, List<Check> checklist, String rationale) {
        if (checklist == null || new HashSet<>(checklist).size() != checklist.size()) throw new InvalidBatch();
        return new Envelope(
                AdministrativeContracts.action(action), reviewKind(kind), new HashSet<>(checks(checklist)), rationale);
    }

    private static List<Check> sorted(Set<EditorialDecision.Check> checks) {
        return checks.stream().sorted().map(c -> map(c, Check.class)).toList();
    }
}

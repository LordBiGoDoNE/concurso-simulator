package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Decisões editoriais em lote (#177): prévia sem escrita e aplicação tudo-ou-nada do estado
 * confirmado. Cada transição passa pelo domínio, como em {@link DecideQuestionUseCase}; a
 * plataforma não procura conflitos editoriais — só recusa divergência entre prévia e aplicação.
 */
public final class EditorialBatchUseCase {
    public static final int MAX_ITEMS = 100;
    public static final int MAX_RATIONALE = 3500;
    public static final long TTL_SECONDS = 900;
    private final AdministrativeQuestionCatalog catalog;
    private final EditorialBatchStore store;
    private final Clock clock;

    public EditorialBatchUseCase(AdministrativeQuestionCatalog catalog, EditorialBatchStore store, Clock clock) {
        this.catalog = Objects.requireNonNull(catalog); this.store = Objects.requireNonNull(store); this.clock = Objects.requireNonNull(clock);
    }

    /** Envelope inválido (forma, limites, ação fora da primeira entrega); sem efeitos. */
    public static final class InvalidBatch extends RuntimeException { public InvalidBatch() { super("INVALID_BATCH"); } }
    /** Estado atual difere do confirmado na prévia: nenhum item aplicado. */
    public static final class BatchDiverged extends RuntimeException {
        private final List<UUID> questionIds;
        public BatchDiverged(Collection<UUID> questionIds) { super("BATCH_DIVERGED"); this.questionIds = questionIds.stream().sorted().toList(); }
        public List<UUID> questionIds() { return questionIds; }
    }
    public static final class ConfirmationFailure extends RuntimeException {
        private final boolean expired;
        public ConfirmationFailure(boolean expired) { super("Confirmação inválida"); this.expired = expired; }
        public boolean expired() { return expired; }
    }

    public record Envelope(EditorialDecision.Action action, EditorialDecision.ReviewKind reviewKind,
                           Set<EditorialDecision.Check> checklist, String rationale) {
        public Envelope {
            if (action != EditorialDecision.Action.SUBMIT && action != EditorialDecision.Action.APPROVE) throw new InvalidBatch();
            if (action == EditorialDecision.Action.APPROVE && reviewKind == null) throw new InvalidBatch();
            if (checklist == null || checklist.stream().anyMatch(Objects::isNull)) throw new InvalidBatch();
            checklist = Set.copyOf(checklist);
            if (rationale == null || rationale.isBlank() || rationale.length() > MAX_RATIONALE
                    || rationale.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')
                    || !java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(rationale)) throw new InvalidBatch();
        }
    }
    public record Target(UUID questionId, UUID revisionId, String fingerprint) {}
    /** Estado observado na prévia e confirmado pelo operador; base da verificação na aplicação. */
    public record Expected(UUID questionId, UUID revisionId, String fingerprint, UUID eligibleRevisionId, Integer editorialSequence) {}

    public enum Refusal { QUESTION_NOT_FOUND, REVISION_NOT_FOUND, FINGERPRINT_MISMATCH, INVALID_TRANSITION, CHECKLIST_INCOMPLETE }
    public record PreviewItem(UUID questionId, QuestionOrigin origin, UUID revisionId, String fingerprint, Question.Status currentStatus,
                              Question.Status resultingStatus, UUID eligibleRevisionId, Integer editorialSequence, Refusal refusal) {}
    public record Preview(Envelope envelope, List<PreviewItem> items, List<Expected> expected, String digest, boolean confirmable) {}

    public Preview preview(Envelope envelope, List<Target> targets, UUID importReceiptId) {
        Objects.requireNonNull(envelope);
        var selected = resolve(targets, importReceiptId);
        var items = new ArrayList<PreviewItem>(); var expected = new ArrayList<Expected>();
        for (var target : selected) {
            var loaded = catalog.question(target.questionId());
            if (loaded.isEmpty()) { items.add(new PreviewItem(target.questionId(), null, target.revisionId(), target.fingerprint(), null, null, null, null, Refusal.QUESTION_NOT_FOUND)); continue; }
            var q = loaded.get();
            var eligible = q.eligible().map(QuestionRevision::id).orElse(null);
            var revision = q.revisions().stream().filter(r -> r.id().equals(target.revisionId())).findFirst();
            if (revision.isEmpty()) { items.add(new PreviewItem(q.id(), q.origin(), target.revisionId(), target.fingerprint(), null, null, eligible, q.editorialSequence(), Refusal.REVISION_NOT_FOUND)); continue; }
            var fingerprint = target.fingerprint() == null ? revision.get().fingerprint() : target.fingerprint();
            var current = q.status(revision.get().id());
            var refusal = simulate(q, envelope, revision.get().id(), fingerprint, clock.instant(), "simulação");
            var resulting = refusal == null ? (envelope.action() == EditorialDecision.Action.SUBMIT ? Question.Status.IN_REVIEW : Question.Status.APPROVED) : null;
            items.add(new PreviewItem(q.id(), q.origin(), revision.get().id(), fingerprint, current, resulting, eligible, q.editorialSequence(), refusal));
            if (refusal == null) expected.add(new Expected(q.id(), revision.get().id(), fingerprint, eligible, q.editorialSequence()));
        }
        boolean confirmable = items.stream().allMatch(i -> i.refusal() == null);
        return new Preview(envelope, List.copyOf(items), confirmable ? List.copyOf(expected) : List.of(), confirmable ? digest(envelope, expected) : null, confirmable);
    }

    public record Applied(UUID questionId, UUID revisionId, String fingerprint, int decisionSequence, Question.Status resultingStatus) {}
    public record Receipt(BatchConfirmation confirmation, Instant committedAt, EditorialDecision.Action action,
                          EditorialDecision.ReviewKind reviewKind, String responsible, List<Applied> decisions) {
        public Receipt { decisions = List.copyOf(decisions); }
    }
    public record Outcome(Receipt receipt, boolean replayed) {}

    public Outcome apply(Envelope envelope, List<Expected> expected, BatchConfirmation confirmation, UUID actor, String destination) {
        Objects.requireNonNull(envelope);
        if (expected == null || expected.isEmpty() || expected.size() > MAX_ITEMS) throw new InvalidBatch();
        var ids = new HashSet<UUID>();
        for (var e : expected) if (e == null || e.questionId() == null || e.revisionId() == null || e.fingerprint() == null
                || e.editorialSequence() == null || e.editorialSequence() < 0 || !ids.add(e.questionId())) throw new InvalidBatch();
        if (!confirmation.actor().equals(actor) || !confirmation.destination().equals(destination)
                || !confirmation.digest().equals(digest(envelope, expected))) throw new ConfirmationFailure(false);
        var now = clock.instant();
        if (confirmation.issuedAt().isAfter(now) || !confirmation.expiresAt().isAfter(now)
                || !confirmation.expiresAt().equals(confirmation.issuedAt().plusSeconds(TTL_SECONDS))) throw new ConfirmationFailure(true);
        try { return applyOnce(envelope, expected, confirmation, actor); }
        catch (EditorialBatchStore.Collision concurrentReplay) { return applyOnce(envelope, expected, confirmation, actor); }
    }

    private Outcome applyOnce(Envelope envelope, List<Expected> expected, BatchConfirmation confirmation, UUID actor) {
        return store.batchTransaction(session -> {
            var previous = session.batchReceipt(confirmation.nonce());
            if (previous.isPresent()) {
                if (!previous.get().confirmation().equals(confirmation)) throw new ConfirmationFailure(false);
                return new Outcome(previous.get(), true);
            }
            // Ordem fixa de travas evita deadlock entre lotes com as mesmas questões.
            var ordered = expected.stream().sorted(Comparator.comparing(Expected::questionId)).toList();
            var loaded = new LinkedHashMap<UUID, Question>(); var diverged = new ArrayList<UUID>();
            for (var e : ordered) {
                var q = session.loadForUpdate(e.questionId());
                if (q.isEmpty() || !Objects.equals(q.get().eligible().map(QuestionRevision::id).orElse(null), e.eligibleRevisionId())
                        || q.get().editorialSequence() != e.editorialSequence().intValue()) { diverged.add(e.questionId()); continue; }
                loaded.put(e.questionId(), q.get());
            }
            // Replay concorrente: a outra aplicação do mesmo token pode ter feito commit enquanto esperávamos as
            // travas. Com as travas adquiridas, uma nova leitura enxerga esse commit e devolve o recibo original.
            var committed = session.batchReceipt(confirmation.nonce());
            if (committed.isPresent()) {
                if (!committed.get().confirmation().equals(confirmation)) throw new ConfirmationFailure(false);
                return new Outcome(committed.get(), true);
            }
            if (!diverged.isEmpty()) throw new BatchDiverged(diverged);
            var at = clock.instant(); var responsible = "admin:" + actor;
            var rationale = envelope.rationale() + " [lote " + confirmation.nonce() + "]";
            var applied = new ArrayList<Applied>();
            for (var e : ordered) {
                var q = loaded.get(e.questionId());
                var decision = new EditorialDecision(envelope.action(), e.questionId(), e.revisionId(), e.fingerprint(), responsible, at,
                        envelope.checklist(), rationale, envelope.reviewKind());
                DecideQuestionUseCase.decideInSession(session, q, decision, e.eligibleRevisionId(), e.editorialSequence());
                applied.add(new Applied(q.id(), e.revisionId(), e.fingerprint(), q.editorialSequence() - 1, q.status(e.revisionId())));
            }
            var receipt = new Receipt(confirmation, at, envelope.action(), envelope.reviewKind(), responsible, applied);
            session.saveBatch(receipt);
            return new Outcome(receipt, false);
        });
    }

    /** Lista explícita ou recibo de importação; nunca ambos. Até 100 questões, sem repetição. */
    private List<Target> resolve(List<Target> targets, UUID importReceiptId) {
        if ((targets == null) == (importReceiptId == null)) throw new InvalidBatch();
        List<Target> selected;
        if (targets != null) {
            for (var t : targets) if (t == null || t.questionId() == null || t.revisionId() == null
                    || t.fingerprint() == null || !t.fingerprint().matches("[0-9a-f]{64}")) throw new InvalidBatch();
            selected = List.copyOf(targets);
        } else {
            selected = catalog.receipt(importReceiptId).orElseThrow(InvalidBatch::new).stream()
                    .map(r -> new Target(r.questionId(), r.revisionId(), null)).toList();
        }
        if (selected.isEmpty() || selected.size() > MAX_ITEMS) throw new InvalidBatch();
        var ids = new HashSet<UUID>();
        for (var t : selected) if (!ids.add(t.questionId())) throw new InvalidBatch();
        return selected;
    }

    /** Aplica a decisão numa cópia restaurada do agregado: as regras do domínio decidem a recusa. */
    private static Refusal simulate(Question q, Envelope envelope, UUID revisionId, String fingerprint, Instant at, String responsible) {
        var copy = Question.restore(q.id(), q.origin(), q.revisions(), q.decisions());
        if (!copy.revision(revisionId).fingerprint().equals(fingerprint)) return Refusal.FINGERPRINT_MISMATCH;
        if (envelope.action() == EditorialDecision.Action.APPROVE && !envelope.checklist().containsAll(EnumSet.allOf(EditorialDecision.Check.class)))
            return Refusal.CHECKLIST_INCOMPLETE;
        try {
            copy.decide(new EditorialDecision(envelope.action(), q.id(), revisionId, fingerprint, responsible, at, envelope.checklist(), envelope.rationale(), envelope.reviewKind()));
            return null;
        } catch (IllegalStateException | IllegalArgumentException refused) { return Refusal.INVALID_TRANSITION; }
    }

    /** Digest canônico do envelope e do estado confirmado por item (ordenado por questão). */
    public static String digest(Envelope envelope, List<Expected> expected) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                text(out, "editorial-batch/v1"); text(out, envelope.action().name());
                text(out, envelope.reviewKind() == null ? "-" : envelope.reviewKind().name());
                var checks = envelope.checklist().stream().map(Enum::name).sorted().toList();
                out.writeInt(checks.size()); for (var check : checks) text(out, check);
                text(out, envelope.rationale());
                var ordered = expected.stream().sorted(Comparator.comparing(Expected::questionId)).toList();
                out.writeInt(ordered.size());
                for (var e : ordered) {
                    text(out, e.questionId().toString()); text(out, e.revisionId().toString()); text(out, e.fingerprint());
                    text(out, e.eligibleRevisionId() == null ? "-" : e.eligibleRevisionId().toString()); out.writeInt(e.editorialSequence());
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void text(DataOutputStream out, String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
}

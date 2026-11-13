package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Regras do lote sem framework (#177): envelope, digest, vínculo e validade da confirmação. */
class EditorialBatchUseCaseTests {
    static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");
    final UUID actor = UUID.randomUUID();
    final Question q = Question.draft(UUID.randomUUID(), new QuestionOrigin("fixture", "batch-unit"), UUID.randomUUID(), QuestionFixtures.content("Lote unitário"));
    final Fake fake = new Fake(q);

    EditorialBatchUseCase at(Instant now) { return new EditorialBatchUseCase(fake, fake, Clock.fixed(now, ZoneOffset.UTC)); }
    Envelope submit() { return new Envelope(EditorialDecision.Action.SUBMIT, null, Set.of(), "Envio sintético"); }
    List<Expected> expected() { return List.of(new Expected(q.id(), q.latest().id(), q.latest().fingerprint(), null, 0)); }
    BatchConfirmation confirmation(Instant issued, UUID who, String digest) { return new BatchConfirmation(UUID.randomUUID(), who, "dest", digest, issued, issued.plusSeconds(900)); }

    @Test void envelopeAcceptsOnlySubmitAndApproveWithReviewKindForApproval() {
        assertThatThrownBy(() -> new Envelope(EditorialDecision.Action.APPROVE, null, Set.of(EditorialDecision.Check.values()), "x")).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> new Envelope(EditorialDecision.Action.WITHDRAW, null, Set.of(), "x")).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> new Envelope(EditorialDecision.Action.RETURN_TO_DRAFT, null, Set.of(), "x")).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> new Envelope(null, null, Set.of(), "x")).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> new Envelope(EditorialDecision.Action.SUBMIT, null, Set.of(), "bad\u0000")).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> new Envelope(EditorialDecision.Action.SUBMIT, null, Set.of(), "x".repeat(3501))).isInstanceOf(InvalidBatch.class);
        assertThat(new Envelope(EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.AI_ONLY, Set.of(EditorialDecision.Check.values()), "ok").reviewKind())
                .isEqualTo(EditorialDecision.ReviewKind.AI_ONLY);
    }

    @Test void digestBindsEnvelopeAndObservedStateIndependentlyOfOrder() {
        var other = Question.draft(UUID.randomUUID(), new QuestionOrigin("fixture", "batch-unit-2"), UUID.randomUUID(), QuestionFixtures.content("Outro"));
        var a = new Expected(q.id(), q.latest().id(), q.latest().fingerprint(), null, 0);
        var b = new Expected(other.id(), other.latest().id(), other.latest().fingerprint(), null, 0);
        assertThat(EditorialBatchUseCase.digest(submit(), List.of(a, b))).isEqualTo(EditorialBatchUseCase.digest(submit(), List.of(b, a)));
        assertThat(EditorialBatchUseCase.digest(submit(), List.of(a))).isNotEqualTo(EditorialBatchUseCase.digest(submit(), List.of(new Expected(a.questionId(), a.revisionId(), a.fingerprint(), null, 1))));
        assertThat(EditorialBatchUseCase.digest(submit(), List.of(a))).isNotEqualTo(EditorialBatchUseCase.digest(new Envelope(EditorialDecision.Action.SUBMIT, null, Set.of(), "Outra"), List.of(a)));
    }

    @Test void confirmationMustMatchActorDestinationDigestAndBeWithinFifteenMinutes() {
        var digest = EditorialBatchUseCase.digest(submit(), expected());
        assertThatThrownBy(() -> at(NOW).apply(submit(), expected(), confirmation(NOW, UUID.randomUUID(), digest), actor, "dest"))
                .isInstanceOfSatisfying(EditorialBatchUseCase.ConfirmationFailure.class, f -> assertThat(f.expired()).isFalse());
        assertThatThrownBy(() -> at(NOW).apply(submit(), expected(), confirmation(NOW, actor, digest), actor, "outro-destino"))
                .isInstanceOf(EditorialBatchUseCase.ConfirmationFailure.class);
        assertThatThrownBy(() -> at(NOW.plusSeconds(900)).apply(submit(), expected(), confirmation(NOW, actor, digest), actor, "dest"))
                .isInstanceOfSatisfying(EditorialBatchUseCase.ConfirmationFailure.class, f -> assertThat(f.expired()).isTrue());
        assertThatThrownBy(() -> at(NOW.minusSeconds(1)).apply(submit(), expected(), confirmation(NOW, actor, digest), actor, "dest"))
                .isInstanceOfSatisfying(EditorialBatchUseCase.ConfirmationFailure.class, f -> assertThat(f.expired()).isTrue());
        assertThat(fake.units).isZero();
        var applied = at(NOW.plusSeconds(899)).apply(submit(), expected(), confirmation(NOW, actor, digest), actor, "dest");
        assertThat(applied.replayed()).isFalse(); assertThat(q.status(q.latest().id())).isEqualTo(Question.Status.IN_REVIEW);
        assertThat(q.decisions().getLast().responsible()).isEqualTo("admin:" + actor);
        assertThat(q.decisions().getLast().at()).isEqualTo(NOW.plusSeconds(899));
    }

    @Test void previewRefusesThroughDomainRulesWithoutChangingTheAggregate() {
        var approve = new Envelope(EditorialDecision.Action.APPROVE, EditorialDecision.ReviewKind.HUMAN_PEER, Set.of(EditorialDecision.Check.values()), "Aprovação");
        var preview = at(NOW).preview(approve, List.of(new Target(q.id(), q.latest().id(), q.latest().fingerprint())), null);
        assertThat(preview.confirmable()).isFalse(); assertThat(preview.digest()).isNull(); assertThat(preview.expected()).isEmpty();
        assertThat(preview.items().getFirst().refusal()).isEqualTo(Refusal.INVALID_TRANSITION);
        assertThat(q.decisions()).isEmpty(); assertThat(fake.units).isZero();
        var ok = at(NOW).preview(submit(), List.of(new Target(q.id(), q.latest().id(), q.latest().fingerprint())), null);
        assertThat(ok.confirmable()).isTrue(); assertThat(ok.digest()).isEqualTo(EditorialBatchUseCase.digest(submit(), expected()));
        assertThatThrownBy(() -> at(NOW).preview(submit(), List.of(new Target(q.id(), q.latest().id(), q.latest().fingerprint()),
                new Target(q.id(), q.latest().id(), q.latest().fingerprint())), null)).isInstanceOf(InvalidBatch.class);
        assertThatThrownBy(() -> at(NOW).preview(submit(), null, null)).isInstanceOf(InvalidBatch.class);
    }

    static final class Fake implements AdministrativeQuestionCatalog, EditorialBatchStore, EditorialBatchStore.Session {
        final Question q; int units; final Map<UUID, EditorialBatchUseCase.Receipt> receipts = new HashMap<>();
        Fake(Question q) { this.q = q; }
        public Page search(AdministrativeQuestionQueries.Criteria criteria) { throw new UnsupportedOperationException(); }
        public Optional<Question> question(UUID id) { return id.equals(q.id()) ? Optional.of(Question.restore(q.id(), q.origin(), q.revisions(), q.decisions())) : Optional.empty(); }
        public List<ImportLink> imports(UUID id) { return List.of(); }
        public Optional<List<ImportedRevision>> receipt(UUID id) { return Optional.empty(); }
        public Optional<StoredFile> file(String path, String sha) { return Optional.empty(); }
        public <T> T batchTransaction(Function<EditorialBatchStore.Session, T> operation) { units++; return operation.apply(this); }
        public Optional<EditorialBatchUseCase.Receipt> batchReceipt(UUID nonce) { return Optional.ofNullable(receipts.get(nonce)); }
        public void saveBatch(EditorialBatchUseCase.Receipt receipt) { receipts.put(receipt.confirmation().nonce(), receipt); }
        public Optional<Question> loadForUpdate(UUID id) { return id.equals(q.id()) ? Optional.of(q) : Optional.empty(); }
        public Optional<Question> findOriginForUpdate(QuestionOrigin origin) { return Optional.empty(); }
        public void insert(Question question) { throw new UnsupportedOperationException(); }
        public void save(Question question) {}
    }
}

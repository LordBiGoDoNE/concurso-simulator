package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.application.DecideQuestionUseCase;
import br.com.concursosimulator.questionbank.application.port.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import static br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.*;
import static br.com.concursosimulator.questionbank.QuestionFixtures.*;
import static org.assertj.core.api.Assertions.*;

class QuestionEditorialTests {
    Question question() { return Question.draft(UUID.randomUUID(), new QuestionOrigin("fixture", "editorial"), UUID.randomUUID(), content("Fixture")); }
    public static EditorialDecision decision(Question q, UUID revision, EditorialDecision.Action action) {
        return new EditorialDecision(action, q.id(), revision, q.revision(revision).fingerprint(), "Revisor sintético de teste",
                Instant.parse("2026-10-11T00:00:00Z"), Set.of(EditorialDecision.Check.values()), "Checklist somente de fixture");
    }
    @Test void requiresExplicitReviewAndExactCompleteEvidence() {
        var q = question(); var id = q.latest().id();
        assertThat(q.status(id)).isEqualTo(Question.Status.DRAFT); assertThat(q.eligible()).isEmpty();
        assertThatThrownBy(() -> q.decide(decision(q, id, APPROVE))).isInstanceOf(IllegalStateException.class);
        q.decide(decision(q, id, SUBMIT));
        var good = decision(q, id, APPROVE);
        var wrong = new EditorialDecision(APPROVE, q.id(), id, "f".repeat(64), good.responsible(), good.at(), good.checklist(), good.rationale());
        assertThatThrownBy(() -> q.decide(wrong)).isInstanceOf(IllegalArgumentException.class);
        var incomplete = new EditorialDecision(APPROVE, q.id(), id, good.fingerprint(), good.responsible(), good.at(), Set.of(), good.rationale());
        assertThatThrownBy(() -> q.decide(incomplete)).isInstanceOf(IllegalArgumentException.class);
        assertThat(q.status(id)).isEqualTo(Question.Status.IN_REVIEW); assertThat(q.eligible()).isEmpty();
        q.decide(good); assertThat(q.eligible()).contains(q.latest());
    }
    @Test void pendingCorrectionCreatesDraftWithoutReplacingEligibleUntilApprovalAndWithdrawalKeepsHistory() {
        var q = question(); var old = q.latest();
        q.decide(decision(q, old.id(), SUBMIT)); q.decide(decision(q, old.id(), APPROVE));
        var next = q.append(UUID.randomUUID(), content("Correção sintética"));
        assertThat(q.status(next.id())).isEqualTo(Question.Status.DRAFT); assertThat(q.eligible()).contains(old);
        q.decide(decision(q, next.id(), SUBMIT)); q.decide(decision(q, next.id(), RETURN_TO_DRAFT));
        assertThat(q.status(next.id())).isEqualTo(Question.Status.DRAFT); assertThat(q.eligible()).contains(old);
        q.decide(decision(q, next.id(), SUBMIT)); q.decide(decision(q, next.id(), APPROVE));
        assertThat(q.eligible()).contains(next); assertThat(q.status(old.id())).isEqualTo(Question.Status.APPROVED);
        q.decide(decision(q, next.id(), WITHDRAW)); assertThat(q.eligible()).isEmpty();
        assertThatThrownBy(() -> q.decide(decision(q, next.id(), SUBMIT))).isInstanceOf(IllegalStateException.class);
        var restored = Question.restore(q.id(), q.origin(), q.revisions(), q.decisions());
        assertThat(restored.eligible()).isEmpty(); assertThat(restored.revisions()).containsExactly(old, next);
        assertThat(restored.decisions()).isEqualTo(q.decisions());
    }
    @Test void evidenceOfOtherQuestionOrRevisionCannotApproveCurrentContentAndPointerCannotRegress() {
        var q = question(); var old = q.latest(); var next = q.append(UUID.randomUUID(), content("Novo"));
        q.decide(decision(q, old.id(), SUBMIT)); q.decide(decision(q, next.id(), SUBMIT)); q.decide(decision(q, next.id(), APPROVE));
        assertThatThrownBy(() -> q.decide(decision(q, old.id(), APPROVE))).isInstanceOf(IllegalStateException.class);
        var good = decision(q, old.id(), APPROVE);
        var other = new EditorialDecision(APPROVE, UUID.randomUUID(), old.id(), good.fingerprint(), good.responsible(), good.at(), good.checklist(), good.rationale());
        assertThatThrownBy(() -> q.decide(other)).isInstanceOf(IllegalArgumentException.class);
        assertThat(q.eligible()).contains(next);
    }
    @Test void applicationChecksExpectedPointerWithinTheUnitBeforeSaving() {
        var q = question(); var store = new FakeStore(q); var useCase = new DecideQuestionUseCase(store);
        useCase.execute(decision(q, q.latest().id(), SUBMIT), null, q.editorialSequence());
        useCase.execute(decision(q, q.latest().id(), APPROVE), null, q.editorialSequence());
        assertThat(store.saved).isEqualTo(2); assertThat(store.units).isEqualTo(2);
        assertThatThrownBy(() -> useCase.execute(decision(q, q.latest().id(), WITHDRAW), null, q.editorialSequence())).isInstanceOf(StaleQuestionException.class);
        assertThat(store.saved).isEqualTo(2); assertThat(q.eligible()).isPresent();
    }
    static final class FakeStore implements QuestionStore, QuestionStore.Session {
        final Question q; int saved; int units;
        FakeStore(Question q) { this.q = q; }
        public <T> T transaction(Function<Session, T> operation) { units++; return operation.apply(this); }
        public Optional<Question> loadForUpdate(UUID id) { return id.equals(q.id()) ? Optional.of(q) : Optional.empty(); }
        public Optional<Question> findOriginForUpdate(QuestionOrigin origin) { return Optional.empty(); }
        public void insert(Question q) { throw new UnsupportedOperationException(); }
        public void save(Question q) { saved++; }
    }
}

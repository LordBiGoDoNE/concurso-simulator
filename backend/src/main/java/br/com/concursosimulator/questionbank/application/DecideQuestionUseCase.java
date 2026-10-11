package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.application.port.StaleQuestionException;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import java.util.Objects;
import java.util.UUID;

public final class DecideQuestionUseCase {
    private final QuestionStore store;
    public DecideQuestionUseCase(QuestionStore store) { this.store = Objects.requireNonNull(store); }
    /** expectedEligible pode ser null para aprovação inicial; não é bypass do controle de concorrência. */
    public void execute(EditorialDecision decision, UUID expectedEligible, int expectedEditorialSequence) {
        if (expectedEditorialSequence < 0) throw new IllegalArgumentException("Sequência editorial esperada inválida");
        store.transaction(session -> {
            var question = session.loadForUpdate(decision.questionId())
                    .orElseThrow(() -> new IllegalArgumentException("Questão inexistente"));
            var actual = question.eligible().map(r -> r.id()).orElse(null);
            if (!Objects.equals(actual, expectedEligible) || question.editorialSequence() != expectedEditorialSequence) {
                throw new StaleQuestionException();
            }
            question.decide(decision); session.save(question); return null;
        });
    }
}

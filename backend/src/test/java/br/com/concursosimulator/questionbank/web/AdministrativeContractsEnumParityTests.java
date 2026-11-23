package br.com.concursosimulator.questionbank.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase;
import br.com.concursosimulator.questionbank.application.PreviewQuestionCandidatesUseCase;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import br.com.concursosimulator.questionbank.domain.Question;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Os enums web de {@link AdministrativeContracts} são convertidos por nome; renomear um valor no núcleo sem
 * ajustar o contrato daria IllegalArgumentException (500) em tempo de execução. Os enums do núcleo expostos
 * como texto (change, match, refusal) mudariam o JSON em silêncio; por isso seus nomes ficam fixados aqui.
 */
class AdministrativeContractsEnumParityTests {
    private static List<String> names(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).toList();
    }

    @Test
    void webEnumsHaveTheSameNamesAsTheirCoreCounterparts() {
        assertThat(names(AdministrativeContracts.Purpose.class)).isEqualTo(names(QuestionPurpose.class));
        assertThat(names(AdministrativeContracts.Status.class)).isEqualTo(names(Question.Status.class));
        assertThat(names(AdministrativeContracts.Action.class)).isEqualTo(names(EditorialDecision.Action.class));
        assertThat(names(AdministrativeContracts.Check.class)).isEqualTo(names(EditorialDecision.Check.class));
        assertThat(names(AdministrativeContracts.ReviewKind.class)).isEqualTo(names(EditorialDecision.ReviewKind.class));
    }

    @Test
    void everyCoreValueRoundTripsThroughTheWebContract() {
        for (var value : QuestionPurpose.values()) assertThat(AdministrativeContracts.purpose(AdministrativeContracts.map(value, AdministrativeContracts.Purpose.class))).isEqualTo(value);
        for (var value : Question.Status.values()) assertThat(AdministrativeContracts.status(AdministrativeContracts.status(value))).isEqualTo(value);
        for (var value : EditorialDecision.Action.values()) assertThat(AdministrativeContracts.action(AdministrativeContracts.map(value, AdministrativeContracts.Action.class))).isEqualTo(value);
        for (var value : EditorialDecision.ReviewKind.values()) assertThat(AdministrativeContracts.reviewKind(AdministrativeContracts.map(value, AdministrativeContracts.ReviewKind.class))).isEqualTo(value);
        assertThat(AdministrativeContracts.checks(Arrays.stream(EditorialDecision.Check.values()).map(c -> AdministrativeContracts.map(c, AdministrativeContracts.Check.class)).toList()))
                .containsExactly(EditorialDecision.Check.values());
    }

    @Test
    void coreEnumsPublishedAsTextKeepTheirContractNames() {
        assertThat(names(PreviewQuestionCandidatesUseCase.Change.class)).containsExactly("NEW", "IDENTICAL", "CHANGED", "STALE");
        assertThat(names(AdministrativeQuestionQueries.Match.class)).containsExactly("ABSENT", "PRESENT", "SAME_AS_LATEST", "SAME_AS_ELIGIBLE", "DIFFERENT");
        assertThat(names(EditorialBatchUseCase.Refusal.class))
                .containsExactly("QUESTION_NOT_FOUND", "REVISION_NOT_FOUND", "FINGERPRINT_MISMATCH", "INVALID_TRANSITION", "CHECKLIST_INCOMPLETE");
    }
}

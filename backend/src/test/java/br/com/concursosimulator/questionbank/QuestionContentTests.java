package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static br.com.concursosimulator.questionbank.QuestionFixtures.*;

class QuestionContentTests {
    @Test void versionsAndOptionsAreImmutableAndKeepIdentityAndEditorialOrder() {
        var first = content("Texto Unicode: área πr² — uma única classificação para ambos os cargos.");
        var id = UUID.randomUUID(); var revisionId = UUID.randomUUID();
        var question = Question.draft(id, new QuestionOrigin("fixture", "P01-Q01"), revisionId, first);
        var second = question.append(UUID.randomUUID(), content("Outra revisão"));
        assertThat(question.id()).isEqualTo(id); assertThat(second.number()).isEqualTo(2);
        assertThat(question.revision(revisionId).content()).isEqualTo(first);
        assertThat(first.alternatives()).extracting(QuestionContent.Alternative::id).containsExactly(FIRST, SECOND);
        assertThat(first.correctAlternativeId()).isEqualTo(FIRST);
        assertThat(first.classification().roles()).hasSize(2);
        assertThatThrownBy(() -> question.revisions().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> question.append(revisionId, first)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsDuplicateMissingAndNonmemberAnswers() {
        var c = content("Fixture");
        for (var options : List.of(List.<QuestionContent.Alternative>of(), List.of(c.alternatives().getFirst()),
                List.of(c.alternatives().getFirst(), c.alternatives().getFirst()),
                List.of(c.alternatives().getFirst(), new QuestionContent.Alternative(SECOND, c.alternatives().getFirst().content())))) {
            assertThatThrownBy(() -> copy(c, options, FIRST)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> copy(c, c.alternatives(), UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void copiesListsAndFingerprintCoversOrderAnswerAndTextWithoutLossyNormalization() {
        var c = content("é π ≥ 1"); var options = new ArrayList<>(c.alternatives());
        var copied = copy(c, options, FIRST); options.clear();
        assertThat(copied.alternatives()).hasSize(2); assertThat(copied.fingerprint()).isEqualTo(c.fingerprint());
        assertThat(copy(c, c.alternatives().reversed(), FIRST).fingerprint()).isNotEqualTo(c.fingerprint());
        assertThat(copy(c, c.alternatives(), SECOND).fingerprint()).isNotEqualTo(c.fingerprint());
        assertThat(content("é π ≥ 1").fingerprint()).isNotEqualTo(c.fingerprint());
        assertThat(c.statement().text()).isEqualTo("é π ≥ 1");
    }
    @Test void sourcesAndAccessibleResourcesMustBeNamedLocalAndTraceable() {
        assertThatThrownBy(() -> new TextContent(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StudyReference("", URI.create("https://example.test"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StudyReference("Fonte", URI.create("javascript:alert(1)"), null)).isInstanceOf(IllegalArgumentException.class);
        for (var path : List.of("../x.png", "/x.png", "https://example.test/x.png", "a/%2e%2e/x.png", "a\\x.png")) {
            assertThatThrownBy(() -> new TextContent.Resource(path, "image/png", "a".repeat(64), "Descrição")).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new TextContent.Resource("assets/x.png", "image/png", "a".repeat(64), "")).isInstanceOf(IllegalArgumentException.class);
        var c = content("Fixture");
        assertThat(c.provenance().author()).contains("Fixture autoral");
        assertThat(c.references().getFirst().name()).isEqualTo("Referência sintética");
    }
    private QuestionContent copy(QuestionContent c, List<QuestionContent.Alternative> options, UUID correct) {
        return new QuestionContent(c.statement(), options, correct, c.resolution(), c.classification(), c.references(), c.provenance());
    }
}

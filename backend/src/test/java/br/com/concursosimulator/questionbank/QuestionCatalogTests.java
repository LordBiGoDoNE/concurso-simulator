package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.Discipline;
import br.com.concursosimulator.questionbank.domain.QuestionClassification;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import br.com.concursosimulator.questionbank.domain.Topic;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.com.concursosimulator.questionbank.domain.Discipline.*;
import static br.com.concursosimulator.questionbank.domain.TargetRole.*;
import static org.assertj.core.api.Assertions.*;

class QuestionCatalogTests {
    @Test
    void catalogPinsTheFiveDisciplinesAndOnlyTheTwoInitialRoles() {
        assertThat(Arrays.stream(Discipline.values()).map(Discipline::id)).containsExactly(
                "portugues", "matematica-raciocinio", "informatica",
                "conhecimentos-administrativos", "conhecimentos-escolares");
        assertThat(TargetRole.CONTEST_ID).isEqualTo("limeira-01-2026");
        assertThat(Arrays.stream(TargetRole.values()).map(TargetRole::id))
                .containsExactly("assistente-administrativo", "secretario-de-escola");
        for (var discipline : Discipline.values()) {
            assertThat(Discipline.fromId(discipline.id())).isSameAs(discipline);
            assertThat(discipline.title()).isNotBlank();
        }
        for (var role : TargetRole.values()) {
            assertThat(TargetRole.fromId(role.id())).isSameAs(role);
            assertThat(role.title()).isNotBlank();
        }
    }

    @Test
    void everyTopicAcceptsOnlyItsOwnDiscipline() {
        for (var topic : Topic.values()) {
            assertThat(Topic.fromId(topic.id())).isSameAs(topic);
            for (var discipline : Discipline.values()) {
                if (topic.discipline() == discipline) {
                    var classification = new QuestionClassification(discipline, topic, Set.of(ADMINISTRATIVE_ASSISTANT));
                    assertThat(classification.topic()).isSameAs(topic);
                } else {
                    assertThatIllegalArgumentException().isThrownBy(() ->
                            new QuestionClassification(discipline, topic, Set.of(ADMINISTRATIVE_ASSISTANT)));
                }
            }
        }
    }

    @Test
    void oneClassificationIsSharedByBothRolesWithoutDuplicatingAssociations() {
        var shared = QuestionClassification.fromIds("portugues", "P01", List.of(
                "assistente-administrativo", "secretario-de-escola", "assistente-administrativo"));
        assertThat(shared.roles()).containsExactlyInAnyOrder(ADMINISTRATIVE_ASSISTANT, SCHOOL_SECRETARY);
        assertThat(shared.appliesTo(ADMINISTRATIVE_ASSISTANT)).isTrue();
        assertThat(shared.appliesTo(SCHOOL_SECRETARY)).isTrue();
        assertThat(shared).isEqualTo(new QuestionClassification(PORTUGUESE, Topic.P01,
                Set.of(SCHOOL_SECRETARY, ADMINISTRATIVE_ASSISTANT)));
        var single = new QuestionClassification(PORTUGUESE, Topic.P01, Set.of(SCHOOL_SECRETARY));
        assertThat(single.appliesTo(ADMINISTRATIVE_ASSISTANT)).isFalse();
        assertThat(single.appliesTo(null)).isFalse();
    }

    @Test
    void classificationMakesADefensiveImmutableCopy() {
        var input = new HashSet<>(Set.of(ADMINISTRATIVE_ASSISTANT));
        var classification = new QuestionClassification(PORTUGUESE, Topic.P01, input);
        input.add(SCHOOL_SECRETARY);
        assertThat(classification.roles()).containsExactly(ADMINISTRATIVE_ASSISTANT);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> classification.roles().add(SCHOOL_SECRETARY));
    }

    @Test
    void missingOrUnknownIdsAreRejectedWithoutNormalizationOrFallback() {
        for (var id : Arrays.asList(null, "", " ", "portugues ", "PORTUGUES", "direito")) {
            assertThatIllegalArgumentException().isThrownBy(() -> Discipline.fromId(id));
        }
        for (var id : Arrays.asList(null, "", "p01", "P01 ", "P11", "X01")) {
            assertThatIllegalArgumentException().isThrownBy(() -> Topic.fromId(id));
        }
        for (var id : Arrays.asList(null, "", "professor", "Assistente Administrativo", "secretario-de-escola ")) {
            assertThatIllegalArgumentException().isThrownBy(() -> TargetRole.fromId(id));
        }
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionClassification.fromIds(
                "portugues", "P01", List.of("assistente-administrativo", "professor")));
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionClassification.fromIds(
                "portugues", "M01", List.of("assistente-administrativo")));
    }

    @Test
    void classificationRequiresDisciplineTopicAndNonemptyNonnullRoles() {
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionClassification(null, Topic.P01, Set.of(SCHOOL_SECRETARY)));
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionClassification(PORTUGUESE, null, Set.of(SCHOOL_SECRETARY)));
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionClassification(PORTUGUESE, Topic.P01, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionClassification(PORTUGUESE, Topic.P01, Set.of()));
        var withNull = new HashSet<TargetRole>();
        withNull.add(null);
        assertThatIllegalArgumentException().isThrownBy(() -> new QuestionClassification(PORTUGUESE, Topic.P01, withNull));
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionClassification.fromIds("portugues", "P01", null));
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionClassification.fromIds("portugues", "P01", List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> QuestionClassification.fromIds("portugues", "P01", Arrays.asList((String) null)));
    }

    @Test
    void everyManifestModuleMapsOnceToItsDocumentedTopicAndDiscipline() throws Exception {
        // Só lê o legado; não extrai questões, altera arquivos ou aprova conteúdo.
        var root = Path.of(".."); // Gradle fixa workingDir no backend.
        var modules = JsonMapper.builder().build().readTree(Files.readString(root.resolve("manifesto_v3.json"))).get("modulos");
        var matrix = Files.readString(root.resolve("docs/question-bank-catalog.md"));
        var byDirectory = Map.of("01_Portugues", PORTUGUESE, "02_Matematica_Logica", MATHEMATICS_LOGIC,
                "03_Informatica", COMPUTING, "04_Assistente_Administrativo", ADMINISTRATIVE_KNOWLEDGE,
                "05_Secretario_Escola", SCHOOL_KNOWLEDGE);
        var seen = new HashSet<String>();
        assertThat(modules.size()).isEqualTo(51);
        for (var module : modules) {
            var id = module.get("id").asString();
            assertThat(seen.add(id)).as("módulo único %s", id).isTrue();
            var topic = Topic.fromId(id);
            var path = module.get("conteudo").asString();
            assertThat(topic.discipline()).isEqualTo(byDirectory.get(path.split("/")[0]));
            assertThat(module.get("modulo").asString()).endsWith(" - " + topic.title());
            var row = "| " + id + " | " + topic.discipline().id() + " | " + topic.title() + " |";
            assertThat(matrix.lines().filter(line -> line.startsWith(row)).count()).isEqualTo(1);
            assertThat(Files.isRegularFile(root.resolve(path))).isTrue();
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(Arrays.stream(Topic.values()).map(Topic::id).toList());
        assertThat(matrix.lines().filter(line -> line.matches("\\| [PMIAS]\\d{2} \\|.*")).count()).isEqualTo(51);
        assertThat(Arrays.stream(Topic.values()).filter(topic -> topic.discipline() == PORTUGUESE).count()).isEqualTo(10);
        assertThat(Arrays.stream(Topic.values()).filter(topic -> topic.discipline() == MATHEMATICS_LOGIC).count()).isEqualTo(12);
        assertThat(Arrays.stream(Topic.values()).filter(topic -> topic.discipline() == COMPUTING).count()).isEqualTo(10);
        assertThat(Arrays.stream(Topic.values()).filter(topic -> topic.discipline() == ADMINISTRATIVE_KNOWLEDGE).count()).isEqualTo(8);
        assertThat(Arrays.stream(Topic.values()).filter(topic -> topic.discipline() == SCHOOL_KNOWLEDGE).count()).isEqualTo(11);
    }
}

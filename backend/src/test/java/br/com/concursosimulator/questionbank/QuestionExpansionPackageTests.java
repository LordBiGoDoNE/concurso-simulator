package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionInventory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

/** Interoperabilidade Python → leitor/digest/prévia/aplicação existentes; só fixtures. */
@Testcontainers
class QuestionExpansionPackageTests {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");
    static final Path FIXTURES = Path.of("build/question-expansion-fixtures");
    static final JsonMapper JSON = JsonMapper.builder().build();
    final Clock clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);
    QuestionDatabase db;

    @BeforeAll static void compileSyntheticPackages() throws Exception {
        compileSyntheticPackages(FIXTURES);
    }
    static void compileSyntheticPackages(Path destination) throws Exception {
        var process = new ProcessBuilder("python", "../scripts/tests/test_question_expansion.py",
                "--fixture-output", destination.toString()).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
    }
    @BeforeEach void open() { db = new QuestionDatabase(PG); }
    @AfterEach void close() { db.close(); }

    @Test void generatedPackagesHaveMatchingFingerprintsAndBinaryDigests() throws Exception {
        int expectedCount = 0;
        for (var purpose : QuestionPurpose.values()) {
            var tree = JSON.readTree(Files.readAllBytes(FIXTURES.resolve(purpose.name()+".json")));
            var request = tree.get("request");
            var document = JSON.treeToValue(request.get("package"), AdministrativePackageReader.Document.class);
            var keys = request.get("keys").valueStream().map(n -> n.asString()).toList();
            var pack = new AdministrativePackageReader().read(document, keys);
            assertThat(pack.purpose()).isEqualTo(purpose);
            assertThat(pack.candidates()).hasSize(100);
            assertThat(pack.digest()).isEqualTo(tree.get("digests").get("packageDigest").asString());
            assertThat(pack.selectionDigest()).isEqualTo(tree.get("digests").get("selectionDigest").asString());
            var inventory = new JpaQuestionInventory(SharedEntityManagerCreator.createSharedEntityManager(db.factory), db.manager);
            var preview = new PreviewQuestionCandidatesUseCase(inventory).execute(pack.candidates());
            assertThat(preview).hasSize(100);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isEqualTo(expectedCount);
            var confirm = new ImportConfirmation(UUID.randomUUID(), UUID.randomUUID(), "isolated-173-package-test",
                    pack.digest(), pack.selectionDigest(), clock.instant(), clock.instant().plusSeconds(900));
            var apply = new ApplyAdministrativeQuestionsUseCase(db.store, clock);
            var first = apply.execute(pack, confirm, confirm.actor(), confirm.destination());
            assertThat(first.replayed()).isFalse();
            var replay = apply.execute(pack, confirm, confirm.actor(), confirm.destination());
            assertThat(replay.replayed()).isTrue();
            assertThat(replay.receipt()).isEqualTo(first.receipt());
            for (var result : first.receipt().results()) {
                var q = db.store.transaction(s -> s.loadForUpdate(result.questionId()).orElseThrow());
                assertThat(q.status(q.latest().id())).isEqualTo(Question.Status.DRAFT);
                assertThat(q.eligible()).isEmpty();
                assertThat(q.decisions()).isEmpty();
            }
            // Uma base por origem; a segunda modalidade acumula sem misturar namespaces.
            expectedCount += 100;
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question", Integer.class))
                    .isEqualTo(expectedCount);
        }
    }
}

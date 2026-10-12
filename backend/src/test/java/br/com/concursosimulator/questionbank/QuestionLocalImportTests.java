package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.infrastructure.local.*;
import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionInventory;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import static org.assertj.core.api.Assertions.*;

/** Banco/schema isolados; comandos de escrita usam somente conteúdo sintético identificado. */
@Testcontainers
class QuestionLocalImportTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6").withPassword("isolated-fixture-secret-not-logged");
    @TempDir static Path TEMP;
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
    static byte[] extracted;
    @BeforeAll static void extractPythonPackage() throws Exception {
        var file = TEMP.resolve("extracted.json");
        var process = new ProcessBuilder("python3", "-B", "scripts/extract_questions.py", "--root", ".")
                .directory(ROOT.toFile()).redirectOutput(file.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        assertThat(process.waitFor()).isZero(); extracted = Files.readAllBytes(file);
    }
    static Path fixturePackage() throws Exception {
        var pack = (ObjectNode) JSON.readTree(extracted);
        var item = (ObjectNode) pack.get("items").get(0);
        var source = JSON.treeToValue(item.get("content"), QuestionPackageReader.Content.class);
        var fixture = QuestionFixtures.content("Fixture CLI: quanto é 2 + 2? πr² e é");
        var domain = new QuestionContent(fixture.statement(), fixture.alternatives(), fixture.correctAlternativeId(), fixture.resolution(),
                fixture.classification(), fixture.references(), source.provenance());
        var content = new QuestionPackageReader.Content(domain.statement(), domain.alternatives(), domain.correctAlternativeId(), domain.resolution(),
                source.classification(), domain.references(), domain.provenance());
        item.set("content", JSON.valueToTree(content)); item.put("fingerprint", domain.fingerprint());
        var file = Files.createTempFile(TEMP, "fixture-", ".json"); Files.writeString(file, JSON.writeValueAsString(pack)); return file;
    }
    static String[] args(String action, Path file, String keys) {
        return new String[]{action, "--editorial-local", "true", "--package", file.toString(), "--root", ROOT.toString(), "--keys", keys};
    }
    static Map<String,String> env(QuestionDatabase db) {
        var schema = db.jdbc.queryForObject("select current_schema()", String.class);
        return Map.of("DB_URL", "jdbc:postgresql://127.0.0.1:" + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName() + "?currentSchema=" + schema,
                "DB_USERNAME", POSTGRES.getUsername(), "DB_PASSWORD", POSTGRES.getPassword());
    }
    static String confirmation(Map<String,String> env, Path file) throws Exception {
        return "APPLY " + env.get("DB_URL") + " " + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))) + "\n";
    }
    @Test void pythonPackageMatchesJavaCatalogAndAll509FingerprintsWithoutApproving() throws Exception {
        var tree = JSON.readTree(extracted); var keys = new HashSet<String>();
        for (var item : tree.get("items")) keys.add(item.get("origin").get("key").asString());
        var selected = new QuestionPackageReader().read(extracted, ROOT, keys);
        assertThat(selected.candidates()).hasSize(509);
        assertThat(selected.inventory()).hasSize(510); assertThat(selected.issues()).hasSize(1);
        assertThat(selected.candidates()).allSatisfy(c -> assertThat(c.expectedLatestRevision()).isNull());
        assertThatThrownBy(() -> new QuestionPackageReader().read(extracted, ROOT, Set.of("P01-Q01", "P05-Q01"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void previewHasNoWritesAndImportNeedsConfirmationAndReusesIds() throws Exception {
        try (var db = new QuestionDatabase(POSTGRES)) {
            var file = fixturePackage(); var env = env(db); var output = new ByteArrayOutputStream();
            assertThat(QuestionBankLocal.run(args("preview", file, "P01-Q01"), env, new BufferedReader(new StringReader("")), new PrintStream(output))).isZero();
            assertThat(output.toString()).contains("NEW", "Inventário: 510").doesNotContain(POSTGRES.getPassword());
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isZero();
            assertThat(QuestionBankLocal.run(args("import", file, "P01-Q01"), env, new BufferedReader(new StringReader("yes\n")), new PrintStream(output))).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isZero();
            for (int i=0; i<2; i++) assertThat(QuestionBankLocal.run(args("import", file, "P01-Q01"), env,
                    new BufferedReader(new StringReader(confirmation(env,file))), new PrintStream(output))).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question", Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_revision", Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_decision", Integer.class)).isZero();
            assertThat(output.toString()).contains("\"reused\":true");
            var read = new JpaQuestionInventory(SharedEntityManagerCreator.createSharedEntityManager(db.factory), db.manager);
            var candidate = new QuestionPackageReader().read(file, ROOT, Set.of("P01-Q01")).candidates().getFirst();
            assertThat(new PreviewQuestionCandidatesUseCase(read).execute(List.of(candidate)).getFirst().change()).isEqualTo(PreviewQuestionCandidatesUseCase.Change.IDENTICAL);
            assertThat(read.read().getFirst().eligibleRevisionId()).isNull();
            var entry = read.read().getFirst(); var revision = entry.latest();
            var decide = new DecideQuestionUseCase(db.store);
            for (var action : List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE,EditorialDecision.Action.WITHDRAW)) {
                var current = read.read().getFirst();
                decide.execute(new EditorialDecision(action,current.questionId(),revision.id(),revision.fingerprint(),"Responsável fixture sintética",
                        java.time.Instant.now(),Set.of(EditorialDecision.Check.values()),"Teste isolado, não aprovação do acervo"),current.eligibleRevisionId(),current.editorialSequence());
            }
            assertThat(QuestionBankLocal.run(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader(confirmation(env,file))),new PrintStream(output))).isZero();
            assertThat(read.read().getFirst().eligibleRevisionId()).isNull();
            assertThat(read.read().getFirst().editorialSequence()).isEqualTo(3);
            // Conteúdo diferente sem base é stale; depois recebe UUID exato da revisão persistida.
            var tree = (ObjectNode) JSON.readTree(Files.readString(file)); var item = (ObjectNode) tree.get("items").get(0);
            ((ObjectNode)item.get("content").get("statement")).put("text","Fixture sintética corrigida");
            var dto = JSON.treeToValue(item.get("content"),QuestionPackageReader.Content.class);
            var changed = new QuestionContent(dto.statement(),dto.alternatives(),dto.correctAlternativeId(),dto.resolution(),candidate.content().classification(),dto.references(),dto.provenance());
            item.put("fingerprint",changed.fingerprint()); Files.writeString(file,JSON.writeValueAsString(tree));
            assertThatThrownBy(() -> QuestionBankLocal.run(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader(confirmation(env,file))),new PrintStream(output)))
                    .isInstanceOf(br.com.concursosimulator.questionbank.application.port.StaleQuestionException.class);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_revision",Integer.class)).isEqualTo(1);
            item.put("expectedLatestRevision",revision.id().toString()); Files.writeString(file,JSON.writeValueAsString(tree));
            assertThat(QuestionBankLocal.run(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader(confirmation(env,file))),new PrintStream(output))).isZero();
            assertThat(read.read().getFirst().revisions()).hasSize(2);
            assertThat(read.read().getFirst().latest().status()).isEqualTo(Question.Status.DRAFT);
            assertThat(read.read().getFirst().eligibleRevisionId()).isNull();
        }
    }
    @Test void invalidSelectionAndUnsafePayloadFailBeforeAnyDatabaseAccess() throws Exception {
        var file = fixturePackage(); var env = Map.of("DB_URL", "jdbc:postgresql://127.0.0.1:1/nonexistent", "DB_USERNAME", "fixture", "DB_PASSWORD", "not-logged");
        assertThatThrownBy(() -> QuestionBankLocal.run(args("import",file,"P01-Q01,P05-Q01"), env,
                new BufferedReader(new StringReader("")), System.out)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inválido");
        for (var unsafe : List.of("<script>bad()</script>", "<span data-correct=\"true\">Texto</span>")) {
            var tree = (ObjectNode) JSON.readTree(Files.readString(file)); var item = (ObjectNode) tree.get("items").get(0);
            ((ObjectNode)item.get("content").get("statement")).put("text",unsafe);
            var dto = JSON.treeToValue(item.get("content"),QuestionPackageReader.Content.class);
            var domain = new QuestionContent(dto.statement(),dto.alternatives(),dto.correctAlternativeId(),dto.resolution(),
                    QuestionClassification.fromIds(dto.classification().disciplineId(),dto.classification().topicId(),dto.classification().roleIds()),dto.references(),dto.provenance());
            item.put("fingerprint",domain.fingerprint());
            assertThatThrownBy(() -> new QuestionPackageReader().read(JSON.writeValueAsBytes(tree),ROOT,Set.of("P01-Q01"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("markup");
        }
        var tree = (ObjectNode) JSON.readTree(Files.readString(file)); ((ObjectNode)tree.get("items").get(0)).put("status","APPROVED");
        assertThatThrownBy(() -> new QuestionPackageReader().read(JSON.writeValueAsBytes(tree),ROOT,Set.of("P01-Q01"))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> QuestionBankLocal.run(args("preview",file,"P01-Q01"),Map.of("DB_URL","jdbc:postgresql://remote.test/db?password=secret"),new BufferedReader(new StringReader("")),System.out)).isInstanceOf(IllegalArgumentException.class);
    }
}

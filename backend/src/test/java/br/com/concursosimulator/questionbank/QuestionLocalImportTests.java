package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.infrastructure.local.*;
import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionInventory;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
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
        var fixture = QuestionFixtures.content("Fixture CLI: x < y e y > z; a < b > c; x < i > y; (x)_(1) + (y)_(2); 2^(3), πr² e é");
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
            assertThat(output.toString()).contains("P05-Q01","SOURCE_ITEM_ISSUE","Fundamentos legais pendentes:","Suspeitas de duplicação");
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
            // Origem/proveniência continuam P01; classificação editorial é versionável.
            ((ObjectNode)item.get("content").get("classification")).put("topicId","P02");
            item.put("expectedLatestRevision",read.read().getFirst().latest().id().toString());
            var reclassified = new QuestionContent(changed.statement(),changed.alternatives(),changed.correctAlternativeId(),changed.resolution(),
                    new QuestionClassification(Discipline.PORTUGUESE,Topic.P02,changed.classification().roles()),changed.references(),changed.provenance());
            item.put("fingerprint",reclassified.fingerprint());Files.writeString(file,JSON.writeValueAsString(tree));
            assertThat(QuestionBankLocal.run(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader(confirmation(env,file))),new PrintStream(output))).isZero();
            assertThat(read.read().getFirst().revisions()).hasSize(3);
            assertThat(read.read().getFirst().latest().classification().topic()).isEqualTo(Topic.P02);
            assertThat(read.read().getFirst().origin().key()).isEqualTo("P01-Q01");
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
            assertThatThrownBy(() -> new QuestionPackageReader().read(JSON.writeValueAsBytes(tree),ROOT,Set.of("P01-Q01"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UNSAFE_CONTENT");
        }
        var tree = (ObjectNode) JSON.readTree(Files.readString(file)); ((ObjectNode)tree.get("items").get(0)).put("status","APPROVED");
        assertThatThrownBy(() -> new QuestionPackageReader().read(JSON.writeValueAsBytes(tree),ROOT,Set.of("P01-Q01"))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> QuestionBankLocal.run(args("preview",file,"P01-Q01"),Map.of("DB_URL","jdbc:postgresql://remote.test/db?password=secret"),new BufferedReader(new StringReader("")),System.out)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void launcherReportsInvalidKeysWithoutPayloadCredentialsOrDatabaseAccess() throws Exception {
        var file=fixturePackage();var tree=(ObjectNode)JSON.readTree(Files.readString(file));
        ((ObjectNode)tree.get("issues").get(0)).put("message","PRIVATE_MESSAGE_SENTINEL jdbc password=secret");Files.writeString(file,JSON.writeValueAsString(tree));
        var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
        var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/unreachable","DB_USERNAME","fixture","DB_PASSWORD","PRIVATE_PASSWORD_SENTINEL");
        assertThat(QuestionBankLocal.launch(args("import",file,"P01-Q01,P05-Q01"),env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err))).isEqualTo(1);
        assertThat(out.size()).isZero();assertThat(err.toString()).contains("P05-Q01","INVALID_SELECTED_ITEM").doesNotContain("PRIVATE_","secret","jdbc:");
        err.reset();
        assertThat(QuestionBankLocal.launch(args("preview",file,"PRIVATE_KEY_SECRET"),env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err))).isEqualTo(1);
        assertThat(err.toString()).contains("INVALID_KEY","UNKNOWN_SELECTED_KEY").doesNotContain("PRIVATE_KEY_SECRET");
        var baseline=Files.readString(file);
        for(var invalidContent:List.of("answer","alternatives")) {
            var bad=(ObjectNode)JSON.readTree(baseline);var item=(ObjectNode)bad.get("items").get(0);
            var content=(ObjectNode)item.get("content");
            if(invalidContent.equals("answer")) content.put("correctAlternativeId",UUID.randomUUID().toString());
            else ((ArrayNode)content.get("alternatives")).add(content.get("alternatives").get(0));
            Files.writeString(file,JSON.writeValueAsString(bad));err.reset();out.reset();
            assertThat(QuestionBankLocal.launch(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err))).isEqualTo(1);
            assertThat(out.size()).isZero();assertThat(err.toString()).contains("P01-Q01","INVALID_QUESTION_CONTENT").doesNotContain("PRIVATE_","secret","jdbc:");
        }
        for(var field:List.of("statement","resolution","alternative")) {
            var bad=(ObjectNode)JSON.readTree(baseline);var content=(ObjectNode)bad.get("items").get(0).get("content");
            var text=(ObjectNode)(field.equals("alternative")?content.get("alternatives").get(0).get("content"):content.get(field));
            text.put("text","");Files.writeString(file,JSON.writeValueAsString(bad));err.reset();out.reset();
            assertThat(QuestionBankLocal.launch(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err))).isEqualTo(1);
            assertThat(out.size()).isZero();assertThat(err.toString()).contains("P01-Q01","INVALID_ITEM_STRUCTURE").doesNotContain("PRIVATE_","secret","jdbc:");
        }
    }
    @Test void launcherDiagnosesKnownOriginFailuresBeforeDatabaseAccess() throws Exception {
        var file=fixturePackage();var baseline=Files.readString(file);
        var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/unreachable","DB_USERNAME","fixture","DB_PASSWORD","PRIVATE_PASSWORD_SENTINEL");
        var cases=Map.of("duplicate","DUPLICATE_ORIGIN","namespace","ORIGIN_NAMESPACE_MISMATCH",
                "outside","ORIGIN_NOT_IN_INVENTORY","invalid","ORIGIN_NOT_CANDIDATE","missing","MISSING_ORIGIN");
        for(var entry:cases.entrySet()) {
            var tree=(ObjectNode)JSON.readTree(baseline);var items=(ArrayNode)tree.get("items");var item=(ObjectNode)items.get(0);
            var key="P01-Q01";
            switch(entry.getKey()) {
                case "duplicate" -> items.add(item.deepCopy());
                case "namespace" -> ((ObjectNode)item.get("origin")).put("namespace","PRIVATE_NAMESPACE_SENTINEL");
                case "outside" -> { key="P01-Q99";((ObjectNode)item.get("origin")).put("key",key); }
                case "invalid" -> { key="P05-Q01";((ObjectNode)item.get("origin")).put("key",key); }
                case "missing" -> { key="INVALID_KEY";item.putNull("origin"); }
            }
            Files.writeString(file,JSON.writeValueAsString(tree));var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
            assertThat(QuestionBankLocal.launch(args("import",file,"P01-Q01"),env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err))).isEqualTo(1);
            assertThat(out.size()).isZero();assertThat(err.toString()).as(entry.getKey()).contains(key,entry.getValue()).doesNotContain("PRIVATE_","jdbc:");
        }
    }
    @Test @ResourceLock(Resources.SYSTEM_ERR)
    void launcherValidatesSvgWithoutRawParserDiagnosticsOrDatabaseAccess() throws Exception {
        var file=fixturePackage();var baseline=Files.readString(file);
        var root=Files.createTempDirectory(TEMP,"svg-root-");
        // Snapshot isolado: não criar nem modificar assets no acervo real.
        for(var source:JSON.readTree(baseline).get("sourceFiles")) {
            var relative=source.get("path").asString();var target=root.resolve(relative);
            Files.createDirectories(target.getParent());Files.copy(ROOT.resolve(relative),target);
        }
        Files.createDirectories(root.resolve("assets"));
        var asset=root.resolve("assets/fixture.svg");
        var valid="<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"1\" height=\"1\"/></svg>";
        var cases=List.of(valid,
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><PRIVATE_SVG_SENTINEL></svg>",
                "<svg xmlns=\"http://www.w3.org/2000/svg\">&PRIVATE_ENTITY_SENTINEL;</svg>",
                "<!DOCTYPE svg [<!ENTITY private SYSTEM \"file:///PRIVATE_FILE_SENTINEL\">]><svg xmlns=\"http://www.w3.org/2000/svg\">&private;</svg>",
                "<?xml-stylesheet type=\"text/css\" href=\"https://example.test/PRIVATE_STYLE_SENTINEL\"?>"+valid,
                valid.replace("</svg>","<?xml-stylesheet href=\"https://example.test/PRIVATE_STYLE_SENTINEL\"?></svg>"),
                valid+"<?xml-stylesheet href=\"https://example.test/PRIVATE_STYLE_SENTINEL\"?>",
                "<?private PRIVATE_PI_SENTINEL?>"+valid);
        try(var destination=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())) {
            destination.setSoTimeout(200);
            var env=Map.of("DB_URL","jdbc:postgresql://localhost:"+destination.getLocalPort()+"/fixture",
                    "DB_USERNAME","fixture","DB_PASSWORD","PRIVATE_PASSWORD_SENTINEL");
            for(var svg:cases) for(var charset:List.of(java.nio.charset.StandardCharsets.UTF_8,java.nio.charset.StandardCharsets.UTF_16)) {
                // A declaração XML não é PI; comentário com texto semelhante também é inerte.
                var xml=svg.equals(valid)?"<?xml version=\"1.0\" encoding=\""+charset.name()+"\"?>"+svg.replace("</svg>","<!-- <?private benign text?> --></svg>"):svg;
                Files.write(asset,xml.getBytes(charset));
                var tree=(ObjectNode)JSON.readTree(baseline);var item=(ObjectNode)tree.get("items").get(0);
                var c=JSON.treeToValue(item.get("content"),QuestionPackageReader.Content.class);
                var hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(asset)));
                var statement=new TextContent(c.statement().text(),List.of(new TextContent.Resource("assets/fixture.svg","image/svg+xml",hash,"Figura sintética")));
                var content=new QuestionContent(statement,c.alternatives(),c.correctAlternativeId(),c.resolution(),
                        QuestionClassification.fromIds(c.classification().disciplineId(),c.classification().topicId(),c.classification().roleIds()),c.references(),c.provenance());
                ((ObjectNode)item.get("content")).set("statement",JSON.valueToTree(statement));item.put("fingerprint",content.fingerprint());
                Files.writeString(file,JSON.writeValueAsString(tree));
                var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();var raw=new ByteArrayOutputStream();
                var arguments=args("import",file,"P01-Q01");arguments[6]=root.toString();
                var previous=System.err;int exit;
                try(var captured=new PrintStream(raw,true,java.nio.charset.StandardCharsets.UTF_8)) {
                    System.setErr(captured);
                    try { exit=QuestionBankLocal.launch(arguments,env,new BufferedReader(new StringReader("")),new PrintStream(out),new PrintStream(err)); }
                    finally { System.setErr(previous); }
                }
                assertThat(raw.toString(java.nio.charset.StandardCharsets.UTF_8)).isEmpty();
                assertThat(err.toString()).doesNotContain("PRIVATE_","jdbc:","<svg","Fatal Error");
                if(svg.equals(valid)) {
                    assertThat(exit).isEqualTo(2);assertThat(err.size()).isZero();
                    assertThat(out.toString()).contains("Confirme digitando exatamente:","Cancelado, sem acesso ao banco.");
                } else {
                    assertThat(exit).isEqualTo(1);assertThat(out.size()).isZero();
                    assertThat(err.toString()).contains("P01-Q01","UNSAFE_CONTENT");
                }
                assertThatThrownBy(destination::accept).isInstanceOf(java.net.SocketTimeoutException.class);
            }
        }
    }
    @Test void reclassificationKeepsIdentityProvenanceAndOldEligibleClassificationUntilApproval() throws Exception {
        try(var db=new QuestionDatabase(POSTGRES)) {
            var file=fixturePackage();var selected=new QuestionPackageReader().read(file,ROOT,Set.of("P01-Q01"));
            var result=new ApplyQuestionCandidatesUseCase(db.store).execute(selected.candidates()).getFirst();
            for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE)) {
                var q=db.store.transaction(s -> s.loadForUpdate(result.questionId()).orElseThrow());
                new DecideQuestionUseCase(db.store).execute(new EditorialDecision(action,q.id(),q.latest().id(),q.latest().fingerprint(),"Fixture reclassificação",java.time.Instant.now(),Set.of(EditorialDecision.Check.values()),"Teste isolado, não curadoria real"),q.eligible().map(QuestionRevision::id).orElse(null),q.editorialSequence());
            }
            var tree=(ObjectNode)JSON.readTree(Files.readString(file));var item=(ObjectNode)tree.get("items").get(0);
            ((ObjectNode)item.get("content").get("classification")).put("topicId","P02");
            var c=selected.candidates().getFirst().content();
            var revised=new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),new QuestionClassification(Discipline.PORTUGUESE,Topic.P02,c.classification().roles()),c.references(),c.provenance());
            item.put("fingerprint",revised.fingerprint());item.put("expectedLatestRevision",result.revisionId().toString());Files.writeString(file,JSON.writeValueAsString(tree));
            var next=new ApplyQuestionCandidatesUseCase(db.store).execute(new QuestionPackageReader().read(file,ROOT,Set.of("P01-Q01")).candidates()).getFirst();
            assertThat(next.questionId()).isEqualTo(result.questionId());
            var q=db.store.transaction(s -> s.loadForUpdate(result.questionId()).orElseThrow());
            assertThat(q.latest().content().provenance()).isEqualTo(c.provenance());
            assertThat(q.latest().content().classification().topic()).isEqualTo(Topic.P02);
            assertThat(q.eligible().orElseThrow().content().classification().topic()).isEqualTo(Topic.P01);
        }
    }
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.questionbank.infrastructure.local.*;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** O lote real é validado/importado SOMENTE como rascunho em PostgreSQL efêmero. */
@Testcontainers
class LessonPracticePackageTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @TempDir static Path temp;
    static final Path ROOT=Path.of("..").toAbsolutePath().normalize();
    static final JsonMapper JSON=JsonMapper.builder().build();
    static Path file;static byte[] bytes;static Set<String> keys;
    @BeforeAll static void compile() throws Exception {
        var python=System.getProperty("os.name").startsWith("Windows")?"python":"python3";
        var process=new ProcessBuilder(python,"-B","scripts/compile_lesson_practice.py","--root",".","--output",temp.toString())
            .directory(ROOT.toFile()).redirectErrorStream(true).start();
        var diagnostic=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).withFailMessage(diagnostic).isZero();file=temp.resolve("package.json");bytes=Files.readAllBytes(file);
        keys=new HashSet<>();for(var item:JSON.readTree(bytes).get("items"))keys.add(item.get("origin").get("key").asString());
    }
    @Test void all255FingerprintsMatchAndCliPreviewImportReplayNeverApprove() throws Exception {
        var selected=new QuestionPackageReader().read(bytes,ROOT,keys);
        assertThat(selected.candidates()).hasSize(255).allSatisfy(c->{assertThat(c.origin().purpose()).isEqualTo(QuestionPurpose.PRATICA_AULA);assertThat(c.expectedLatestRevision()).isNull();});
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("11");var mock=db.seed("Simulado sintético preservado",true);
            var before=db.jdbc.queryForObject("select payload::text from qb_revision where question_id=?",String.class,mock.id());
            var url="jdbc:postgresql://127.0.0.1:"+PG.getMappedPort(5432)+"/"+PG.getDatabaseName()+"?currentSchema="+db.schema;
            var env=Map.of("DB_URL",url,"DB_USERNAME",PG.getUsername(),"DB_PASSWORD",PG.getPassword());
            var out=new PrintStream(new ByteArrayOutputStream());var all=String.join(",",keys.stream().sorted().toList());
            String[] args={"preview","--editorial-local","true","--package",file.toString(),"--root",ROOT.toString(),"--keys",all};
            assertThat(QuestionBankLocal.run(args,env,new BufferedReader(new StringReader("")),out)).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question",Integer.class)).isEqualTo(1);
            args[0]="import";var confirmation="APPLY "+url+" "+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))+"\n";
            assertThat(QuestionBankLocal.run(args,env,new BufferedReader(new StringReader("no\n")),out)).isEqualTo(2);
            for(int i=0;i<2;i++)assertThat(QuestionBankLocal.run(args,env,new BufferedReader(new StringReader(confirmation)),out)).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from qb_question where purpose='PRATICA_AULA'",Integer.class)).isEqualTo(255);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_revision where status='DRAFT'",Integer.class)).isEqualTo(255);
            var ownPool=new org.springframework.transaction.support.TransactionTemplate(db.manager).execute(status->db.pool.capture(QuestionPurpose.PRATICA_AULA));assertThat(ownPool).isEmpty();
            var mockPool=new org.springframework.transaction.support.TransactionTemplate(db.manager).execute(status->db.pool.capture());assertThat(mockPool).hasSize(1);
            assertThat(db.jdbc.queryForObject("select payload::text from qb_revision where question_id=?",String.class,mock.id())).isEqualTo(before);
        }
    }
    @Test void wrongDestinationFingerprintSourcesAndMissingItemsAreRejectedBeforeDb() throws Exception {
        for(String mutation:List.of("namespace","fingerprint","source","missing")) {
            var tree=(ObjectNode)JSON.readTree(bytes);var first=(ObjectNode)tree.get("items").get(0);
            switch(mutation) {
                case "namespace" -> ((ObjectNode)first.get("origin")).put("namespace","legacy-v3");
                case "fingerprint" -> first.put("fingerprint","a".repeat(64));
                case "source" -> ((ObjectNode)tree.get("sourceFiles").get(0)).put("sha256","b".repeat(64));
                case "missing" -> ((tools.jackson.databind.node.ArrayNode)tree.get("items")).remove(0);
            }
            assertThatThrownBy(()->new QuestionPackageReader().read(JSON.writeValueAsBytes(tree),ROOT,keys)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

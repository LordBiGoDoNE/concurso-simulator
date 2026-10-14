package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.infrastructure.local.QuestionBankLocal;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.application.port.StaleQuestionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class QuestionEditorialCliTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    @TempDir Path temp;
    final JsonMapper json = JsonMapper.builder().build();
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    record FixtureCommand(String format, EditorialDecision decision, UUID expectedEligible, int expectedEditorialSequence) {}
    Map<String,String> env(QuestionDatabase db) {
        return Map.of("DB_URL","jdbc:postgresql://127.0.0.1:"+POSTGRES.getMappedPort(5432)+"/"+POSTGRES.getDatabaseName()+"?currentSchema="+db.jdbc.queryForObject("select current_schema()",String.class),
                "DB_USERNAME",POSTGRES.getUsername(),"DB_PASSWORD",POSTGRES.getPassword());
    }
    Path command(Question q, EditorialDecision.Action action, Set<EditorialDecision.Check> checklist, String fingerprint, int sequence, UUID expected) throws Exception {
        var decision = new EditorialDecision(action,q.id(),q.latest().id(),fingerprint,"Responsável sintético de teste",Instant.now(),checklist,"Fixture isolada, sem aprovação do conteúdo real");
        var file = Files.createTempFile(temp,"decision-", ".json"); Files.writeString(file,json.writeValueAsString(new FixtureCommand("question-decision/v1",decision,expected,sequence)));return file;
    }
    String confirm(Map<String,String> env, Path file) throws Exception {
        return "DECIDE " + env.get("DB_URL") + " " + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))) + "\n";
    }
    int decide(Path file, Map<String,String> env, String confirmation) throws Exception {
        return QuestionBankLocal.run(new String[]{"decide","--editorial-local","true","--decision",file.toString()},env,new BufferedReader(new StringReader(confirmation)),new PrintStream(output));
    }
    @Test void exactEvidenceChecklistAndSequenceProtectSubmitApproveWithdrawAndPrivateCoverage() throws Exception {
        try(var db=new QuestionDatabase(POSTGRES)) {
            var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("fixture-editorial-cli","isolated-only"),UUID.randomUUID(),QuestionFixtures.content("Fixture editorial, πr²"));
            db.store.transaction(s -> {s.insert(q);return null;});var env=env(db);var all=Set.of(EditorialDecision.Check.values());
            var submit=command(q,EditorialDecision.Action.SUBMIT,Set.of(),q.latest().fingerprint(),0,null);
            assertThat(decide(submit,env,"yes\n")).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_decision",Integer.class)).isZero();
            assertThat(decide(submit,env,confirm(env,submit))).isZero();
            var wrong=command(q,EditorialDecision.Action.APPROVE,all,"a".repeat(64),1,null);
            assertThatThrownBy(() -> decide(wrong,env,confirm(env,wrong))).hasRootCauseInstanceOf(IllegalArgumentException.class);
            var missing=command(q,EditorialDecision.Action.APPROVE,Set.of(EditorialDecision.Check.CORRECTNESS),q.latest().fingerprint(),1,null);
            assertThatThrownBy(() -> decide(missing,env,confirm(env,missing))).hasRootCauseInstanceOf(IllegalArgumentException.class);
            var stale=command(q,EditorialDecision.Action.APPROVE,all,q.latest().fingerprint(),0,null);
            assertThatThrownBy(() -> decide(stale,env,confirm(env,stale))).isInstanceOf(StaleQuestionException.class);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_decision",Integer.class)).isEqualTo(1);
            var approve=command(q,EditorialDecision.Action.APPROVE,all,q.latest().fingerprint(),1,null);
            assertThat(decide(approve,env,confirm(env,approve))).isZero();
            output.reset();
            assertThat(QuestionBankLocal.run(new String[]{"report","--editorial-local","true"},env,new BufferedReader(new StringReader("")),new PrintStream(output))).isZero();
            var report=json.readTree(output.toString().substring(output.toString().indexOf('\n')+1));
            assertThat(report.get("topics").size()).isEqualTo(51);
            assertThat(report.get("disciplines").get(0).get("eligible").asInt()).isEqualTo(1);
            assertThat(report.get("disciplines").get(0).get("imported").asInt()).isEqualTo(1); // dois cargos não duplicam
            assertThat(report.get("questions").get(0).get("editorialSequence").asInt()).isEqualTo(2);
            var withdraw=command(q,EditorialDecision.Action.WITHDRAW,Set.of(),q.latest().fingerprint(),2,q.latest().id());
            assertThat(decide(withdraw,env,confirm(env,withdraw))).isZero();
            assertThat(db.jdbc.queryForObject("select eligible_revision_id from qb_question",UUID.class)).isNull();
            assertThat(db.jdbc.queryForObject("select count(*) from qb_decision",Integer.class)).isEqualTo(3);
            // Repetir aprovação antiga não reativa retirada/ABA.
            assertThatThrownBy(() -> decide(approve,env,confirm(env,approve))).isInstanceOf(StaleQuestionException.class);
        }
    }
    @Test void ciAndWrongDestinationOrModeRefuseBeforeConnecting() throws Exception {
        var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("fixture","only"),UUID.randomUUID(),QuestionFixtures.content("Fixture CI"));
        var approve=command(q,EditorialDecision.Action.APPROVE,Set.of(EditorialDecision.Check.values()),q.latest().fingerprint(),1,null);
        var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/unreachable","DB_USERNAME","fixture","DB_PASSWORD","never-logged","CI","true");
        assertThatThrownBy(() -> decide(approve,env,"" )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CI");
        assertThat(output.size()).isZero();
        var remote=new HashMap<>(env);remote.put("DB_URL","jdbc:postgresql://remote.example/real");
        assertThatThrownBy(() -> decide(approve,remote,"" )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("local");
        assertThatThrownBy(() -> QuestionBankLocal.run(new String[]{"decide","--editorial-local","false","--decision",approve.toString()},env,new BufferedReader(new StringReader("")),new PrintStream(output))).isInstanceOf(IllegalArgumentException.class);
        var tree=(tools.jackson.databind.node.ObjectNode)json.readTree(Files.readString(approve));tree.remove("expectedEditorialSequence");Files.writeString(approve,json.writeValueAsString(tree));
        assertThatThrownBy(() -> decide(approve,env,"" )).isInstanceOf(RuntimeException.class);
    }
}

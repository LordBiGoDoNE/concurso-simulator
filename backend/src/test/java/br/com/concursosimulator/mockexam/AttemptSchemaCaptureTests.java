package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.sql.Timestamp;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class AttemptSchemaCaptureTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void upgradeV4PreservesAccountsSessionsAndBankAndValidatesNewJpaModels() {
        try (var db=new AttemptDatabase(PG,"4")) {
            var id=UUID.randomUUID(); db.jdbc.update("insert into app_user(id) values (?)",id);
            db.jdbc.update("insert into external_identity values (?,'google','https://accounts.google.com','fixture')",id);
            db.jdbc.update("insert into spring_session values (?,?,1,1,1800,1800000,?)",id.toString(),id.toString(),id.toString());
            db.jdbc.update("insert into spring_session_attributes values (?,'fixture',decode('1234','hex'))",id.toString());
            db.jdbc.update("insert into qb_question(id,origin_namespace,origin_key) values (?,'fixture','existing')",id);
            var tables=List.of("app_user","external_identity","spring_session","qb_question");
            var before=tables.stream().map(t -> db.jdbc.queryForList("select * from "+t)).toList();
            db.migrate("5"); db.open();
            assertThat(tables.stream().map(t -> db.jdbc.queryForList("select * from "+t)).toList()).isEqualTo(before);
            assertThat(db.jdbc.queryForObject("select encode(attribute_bytes,'hex') from spring_session_attributes",String.class)).isEqualTo("1234");
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isZero();
        }
    }
    @Test void ownerExpiryVersionAndCommandOwnerConstraintsRejectInvalidRows() {
        try (var db=new AttemptDatabase(PG)) {
            var a=attempt(VISITOR,1).snapshot(); var json=JsonMapper.builder().build();
            var insert="insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,expires_at,payload) values (?,'VISITOR',?,null,?,?,?::jsonb)";
            assertThatThrownBy(() -> db.jdbc.update(insert,a.id(),VISITOR.id(),Timestamp.from(NOW),null,json.writeValueAsString(a))).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update(insert,a.id(),VISITOR.id(),Timestamp.from(NOW),Timestamp.from(NOW.plusSeconds(1)),json.writeValueAsString(a))).isInstanceOf(DataIntegrityViolationException.class);
            db.jdbc.update(insert,a.id(),VISITOR.id(),Timestamp.from(NOW),Timestamp.from(NOW.plusSeconds(86400)),json.writeValueAsString(a));
            assertThatThrownBy(() -> db.jdbc.update("update me_attempt set owner_id=? where id=?",UUID.randomUUID(),a.id())).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("insert into me_command values (?,'VISITOR',?,?,?,?,'{}'::jsonb)",UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),a.id(),"a".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("insert into me_attempt values (?,'ACCOUNT',?,?,?,null,null,0,?::jsonb)",UUID.randomUUID(),ACCOUNT.id(),ACCOUNT.id(),Timestamp.from(NOW),json.writeValueAsString(a))).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
    @Test void captureRequiresConsumerTransactionAndExcludesDraftAndWithdrawnRevisions() {
        try (var db=new AttemptDatabase(PG)) {
            var approved=db.seed("Questão elegível",true); db.seed("Rascunho",false);
            assertThatThrownBy(() -> db.pool.capture()).isInstanceOf(IllegalStateException.class);
            var tx=new TransactionTemplate(db.manager);
            var captured=tx.execute(s -> db.pool.capture());
            assertThat(captured).singleElement().satisfies(e -> {
                assertThat(e.questionId()).isEqualTo(approved.id()); assertThat(e.fingerprint()).isEqualTo(approved.latest().fingerprint());
                assertThat(e.content().correctAlternativeId()).isEqualTo(approved.latest().content().correctAlternativeId());
            });
            db.decide(approved.id(),br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.WITHDRAW);
            var remaining=tx.execute(s -> db.pool.capture()); assertThat(remaining).isEmpty();
            assertThat(captured.getFirst().content()).isEqualTo(approved.latest().content());
        }
    }
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.Topic;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class PracticeCapacityMigrationTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void v15ChangesOnlyNewCapacityAndPreservesFiveItemCapturesCommandsAndLegacyGuards() {
        try(var db=new AttemptDatabase(PG)) {
            db.migrate("14");new LessonPracticePersistenceTests().seedPractice(db,30);
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());
            var store=new JpaAttemptStore(db.entities,db.manager,db.pool);var op=UUID.randomUUID();
            var five=PracticeCapacityOperationalFixture.captureFive(store,op,4);
            var old=PracticeCapacityOperationalFixture.captureFive(store,UUID.randomUUID(),3);
            var tables=List.of("qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command");
            var before=tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
            db.migrate("15");
            assertThat(tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList()).isEqualTo(before);
            var p=LessonPracticeUseCasesTests.practice(store,NOW);
            assertThat(p.createPractice(ACCOUNT,op,Topic.P01).attempt().items()).hasSize(5);
            assertThat(p.read(ACCOUNT,old.id()).items()).hasSize(5);
            var fresh=p.createPractice(ACCOUNT,UUID.randomUUID(),Topic.P01).attempt();
            assertThat(fresh.items()).hasSize(5);
            var previousLarge=PracticeCapacityOperationalFixture.capture(store,UUID.randomUUID(),4,25);
            assertThat(p.read(ACCOUNT,previousLarge.id()).items()).hasSize(25);
            var json=JsonMapper.builder().build();
            for(var change:List.of(Map.entry(fresh.id(),26),Map.entry(old.id(),6))) {
                var payload=(tools.jackson.databind.node.ObjectNode)json.readTree(db.jdbc.queryForObject("select payload::text from me_attempt where id=?",String.class,change.getKey()));
                var id=UUID.randomUUID();payload.put("id",id.toString());
                var items=(tools.jackson.databind.node.ArrayNode)payload.get("items");
                while(items.size()<change.getValue())items.add(items.get(0).deepCopy());
                var failure=catchThrowable(()->db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,expires_at,payload) select ?,owner_kind,owner_id,account_id,created_at,expires_at,?::jsonb from me_attempt where id=?",id,json.writeValueAsString(payload),change.getKey()));
                assertThat(failure).isNotNull();
                var cause=org.springframework.core.NestedExceptionUtils.getMostSpecificCause(failure);
                assertThat(cause).isInstanceOf(java.sql.SQLException.class);
                assertThat(((java.sql.SQLException)cause).getSQLState()).isEqualTo("23514");
                assertThat(cause.getMessage()).contains("\"me_attempt_purpose_valid\"");
                assertThat(db.jdbc.queryForObject("select count(*) from me_attempt where id=?",Integer.class,id)).isZero();
            }
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(4);
        }
    }
}

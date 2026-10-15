package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.mockexam.application.AttemptUseCases;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaEligibleRevisionPool;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class TemplateSchemaTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void v5UpgradePreservesAccountsSessionsBankAttemptsAndCommandsWithoutRewritingPayloads() {
        try(var db=new ParameterDatabase(PG,"5")) {
            db.seed(Family.PERCENTAGE);var owner=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
            db.jdbc.update("insert into app_user(id) values (?)",owner.id());
            db.jdbc.update("insert into external_identity values (?, 'google', 'https://accounts.google.com','isolated-fixture')",owner.id());
            db.jdbc.update("insert into spring_session values (?,?,1,1,1800,1800000,?)",owner.id().toString(),owner.id().toString(),owner.id().toString());
            db.jdbc.update("insert into spring_session_attributes values (?,'fixture',decode('1234','hex'))",owner.id().toString());
            var composition=new Composition(TargetRole.SCHOOL_SECRETARY,Map.of(Discipline.MATHEMATICS_LOGIC,new Composition.Portion(1,Set.of())));
            var beforeCases=attempts(db);var command=UUID.randomUUID();var created=beforeCases.create(owner,command,composition).attempt();
            var tables=List.of("app_user","external_identity","spring_session","spring_session_attributes","qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command");
            var before=new HashMap<String,List<String>>();
            for(var table:tables)before.put(table,db.jdbc.queryForList("select row_to_json(t)::text from "+table+" t order by 1",String.class));
            var trigger=db.jdbc.queryForObject("select pg_get_functiondef('me_protect_attempt()'::regprocedure)",String.class);
            db.factory.close();db.factory=null;db.migrate("6");db.open(true);
            for(var table:tables)assertThat(db.jdbc.queryForList("select row_to_json(t)::text from "+table+" t order by 1",String.class)).as(table).isEqualTo(before.get(table));
            assertThat(db.jdbc.queryForObject("select pg_get_functiondef('me_protect_attempt()'::regprocedure)",String.class)).isEqualTo(trigger);
            var afterCases=attempts(db);assertThat(afterCases.create(owner,command,composition).attempt()).isEqualTo(created);
            var answer=afterCases.answer(owner,created.id(),UUID.randomUUID(),0,created.items().getFirst().id(),null);
            assertThat(afterCases.finish(owner,created.id(),UUID.randomUUID(),answer.attempt().version()).result().blank()).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_family",Integer.class)).isZero();
        }
    }
    @Test void freshSchemaAndJpaValidateAndRejectCrossBaseOrUnapprovedPointers() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);new br.com.concursosimulator.parameterization.application.TemplateUseCases(db.store).register(d,0);
            assertThatThrownBy(() -> db.jdbc.update("update pq_family set eligible_version=1")).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("update pq_family set base_question_id=?",UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
            var wrong=new TemplateDefinition(UUID.randomUUID(),1,d.family(),d.generatorVersion(),d.baseQuestionId(),UUID.randomUUID(),d.baseFingerprint(),d.classification(),d.references(),d.provenance());
            // Outro ID na mesma base já é conflito conhecido e nunca cria outra família.
            assertThatThrownBy(() -> db.store.transaction(s -> {s.insert(new TemplateFamily(br.com.concursosimulator.parameterization.application.TemplateUseCases.preview(wrong)));return null;}))
                    .isInstanceOf(br.com.concursosimulator.parameterization.application.TemplateStore.ConcurrentRegistration.class);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_family",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_version",Integer.class)).isEqualTo(1);
        }
    }
    private static AttemptUseCases attempts(ParameterDatabase db) {
        return new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,new JpaEligibleRevisionPool(db.entities)),Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"),ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42));
    }
}

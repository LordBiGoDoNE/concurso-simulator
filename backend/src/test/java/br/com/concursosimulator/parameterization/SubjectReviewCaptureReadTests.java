package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.*;
import br.com.concursosimulator.parameterization.application.TemplateUseCases;
import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import br.com.concursosimulator.ConcursoSimulatorApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.parameterization.ParameterizedPersistenceTests.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class SubjectReviewCaptureReadTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void fixedAndGeneratedBaseAreDeduplicatedAfterReclassificationWithdrawalUpgradeAndFactoryRestart() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);var version=db.approve(d);
            var owner=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());db.jdbc.update("insert into app_user(id) values (?)",owner.id());
            var fixed=cases(store(db)).create(owner,UUID.randomUUID(),composition(false)).attempt();
            cases(store(db)).finish(owner,fixed.id(),UUID.randomUUID(),0);
            var second=new AttemptUseCases(store(db),Clock.fixed(NOW.plusSeconds(1),ZoneOffset.UTC),UUID::randomUUID,()->new Random(7));
            var generated=second.create(owner,UUID.randomUUID(),composition(true)).attempt();second.finish(owner,generated.id(),UUID.randomUUID(),0);
            new TemplateUseCases(db.store).decide(ParameterFixtures.decision(version,TemplateDecision.Action.WITHDRAW),1,2);
            db.questions.transaction(s -> {
                var q=s.loadForUpdate(d.baseQuestionId()).orElseThrow();var c=q.latest().content();
                q.append(UUID.randomUUID(),new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                        new QuestionClassification(Discipline.MATHEMATICS_LOGIC,Topic.M05,c.classification().roles()),c.references(),c.provenance()));
                s.save(q);return null;
            });
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.SUBMIT);db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.APPROVE);
            var third=new AttemptUseCases(store(db),Clock.fixed(NOW.plusSeconds(2),ZoneOffset.UTC),UUID::randomUUID,()->new Random(8),false);
            var last=third.create(owner,UUID.randomUUID(),composition(false)).attempt();third.finish(owner,last.id(),UUID.randomUUID(),0);
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.WITHDRAW);
            var before=rows(db);db.migrate("10");db.open(true);
            var frozen=new JpaAttemptStore(db.entities,db.manager,()->{throw new AssertionError("Sem catálogo");},()->{throw new AssertionError("Sem templates");});
            var rollback=new AttemptUseCases(frozen,Clock.fixed(NOW.plusSeconds(3),ZoneOffset.UTC),UUID::randomUUID,()->{throw new AssertionError("Sem geração");},false);
            assertThat(rollback.result(owner,generated.id()).blank()).isEqualTo(1);
            var reader=new JpaCompletedAttempts(db.entities,db.manager);var uc=new ReviewUseCases(reader,Clock.fixed(NOW.plusSeconds(3),ZoneOffset.UTC));
            var result=uc.overview(owner,TargetRole.SCHOOL_SECRETARY);assertThat(result.window().attemptCount()).isEqualTo(3);
            var old=result.summary().subjects().stream().filter(m->m.topic()==Topic.M04).findFirst().orElseThrow();
            var recent=result.summary().subjects().stream().filter(m->m.topic()==Topic.M05).findFirst().orElseThrow();
            assertThat(old.exposures()).isEqualTo(2);assertThat(old.assessed()).isZero();assertThat(old.percentage()).isNull();
            assertThat(recent.exposures()).isEqualTo(1);assertThat(recent.blank()).isEqualTo(1);
            var individual=uc.individual(owner,generated.id());
            assertThat(individual.summary().subjects().stream().filter(m->m.topic()==Topic.M04).findFirst().orElseThrow().blank()).isEqualTo(1);
            assertThat(reader.recent(owner,TargetRole.ADMINISTRATIVE_ASSISTANT,100,NOW.plusSeconds(3)).attempts()).isEmpty();
            // Fecha e abre a aplicação real duas vezes, mantendo somente o PostgreSQL sintético.
            for(int restart=0;restart<2;restart++)try(var app=new SpringApplicationBuilder(ConcursoSimulatorApplication.class).run(
                    "--spring.profiles.active=local","--server.address=127.0.0.1","--server.port=0",
                    "--spring.datasource.url="+db.ds.getUrl(),"--spring.datasource.username="+PG.getUsername(),"--spring.datasource.password="+PG.getPassword(),
                    "--parameterization.generation-enabled=false")) {
                assertThat(app.getEnvironment().getProperty("parameterization.generation-enabled",Boolean.class)).isFalse();
                var actual=app.getBean(ReviewUseCases.class).overview(owner,TargetRole.SCHOOL_SECRETARY);
                assertThat(actual.window().attemptCount()).isEqualTo(3);assertThat(actual.summary().subjects()).isEqualTo(result.summary().subjects());
                assertThat(app.getBean(AttemptUseCases.class).result(owner,generated.id()).blank()).isEqualTo(1);
            }
            assertThat(rows(db)).isEqualTo(before);
        }
    }
    @Test void instancePayloadDivergenceIsRejectedByTheSameBatchReader() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.SIMPLE_INTEREST);db.approve(d);
            var created=cases(store(db)).create(VISITOR,UUID.randomUUID(),composition(true)).attempt();
            cases(store(db)).finish(VISITOR,created.id(),UUID.randomUUID(),0);
            db.jdbc.execute("alter table pq_instance disable trigger user");
            db.jdbc.update("update pq_instance set payload=jsonb_set(payload,'{question,content,statement,text}','\"PRIVATE_CORRUPT_CAPTURE\"')");
            db.jdbc.execute("alter table pq_instance enable trigger user");var before=rows(db);
            assertThatThrownBy(()->new JpaCompletedAttempts(db.entities,db.manager).one(VISITOR,created.id(),NOW)).isInstanceOf(IllegalStateException.class);
            assertThat(rows(db)).isEqualTo(before);
        }
    }
    List<List<String>> rows(ParameterDatabase db) {
        return List.of("app_user","qb_question","qb_revision","qb_decision","pq_family","pq_version","pq_decision","pq_instance","me_attempt","me_item","me_command")
                .stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
    }
}

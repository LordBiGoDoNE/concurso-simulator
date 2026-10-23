package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.ExpiredAttemptCleanup;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.IntervalTask;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class AttemptHistoryExpiryTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    JpaAttemptStore store(AttemptDatabase db) { return new JpaAttemptStore(db.entities,db.manager,db.pool); }
    AttemptUseCases cases(AttemptStore store,Instant now) { return new AttemptUseCases(store,Clock.fixed(now,ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42)); }
    void seed(AttemptDatabase db) {
        db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id()); db.seed("Questão de histórico",true);
    }
    @Test void accountHistoryHasStablePagesOwnerFilterStateAndNoPartialGradeAcrossSessions() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var s=store(db); var uc=cases(s,NOW);
            var a=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var b=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var c=cases(s,NOW.plusSeconds(1)).create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var other=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
            db.jdbc.update("insert into app_user(id) values (?)",other.id()); uc.create(other,UUID.randomUUID(),composition(1));
            uc.create(VISITOR,UUID.randomUUID(),composition(1)); uc.finish(ACCOUNT,a.id(),UUID.randomUUID(),0);
            var fresh=cases(store(db),NOW.plusSeconds(2));
            var first=fresh.history(ACCOUNT,new AttemptStore.HistoryQuery(0,2,null));
            var second=fresh.history(ACCOUNT,new AttemptStore.HistoryQuery(1,2,null));
            assertThat(first.getFirst().id()).isEqualTo(c.id());
            var ids=new ArrayList<>(first.stream().map(AttemptUseCases.HistoryEntry::id).toList());
            ids.addAll(second.stream().map(AttemptUseCases.HistoryEntry::id).toList());
            assertThat(ids).containsExactlyElementsOf(db.jdbc.queryForList("select id from me_attempt where owner_kind='ACCOUNT' and owner_id=? order by created_at desc,id desc",UUID.class,ACCOUNT.id()));
            assertThat(ids).doesNotHaveDuplicates().containsExactlyInAnyOrder(a.id(),b.id(),c.id());
            var active=fresh.history(ACCOUNT,new AttemptStore.HistoryQuery(0,10,false));
            assertThat(active).hasSize(2).allSatisfy(e -> assertThat(e.percentage()).isNull());
            assertThat(fresh.history(ACCOUNT,new AttemptStore.HistoryQuery(0,10,true))).singleElement().satisfies(e -> assertThat(e.percentage()).isEqualByComparingTo("0"));
            assertThat(fresh.history(ACCOUNT,new AttemptStore.HistoryQuery(2,2,null))).isEmpty();
            assertThatThrownBy(() -> fresh.history(VISITOR,new AttemptStore.HistoryQuery(0,10,null))).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(() -> fresh.read(other,a.id())).isInstanceOf(Attempt.Unavailable.class);
        }
    }
    @Test void expirationIsExactly24HoursAndStartupCascadesOnlyExpiredTemporaryData() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var s=store(db); var uc=cases(s,NOW);
            var oldOp=UUID.randomUUID(); var guest=uc.create(VISITOR,oldOp,composition(1)).attempt();
            var account=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            uc.finish(VISITOR,guest.id(),UUID.randomUUID(),0);
            var newer=cases(s,NOW.plusSeconds(60)).create(VISITOR,UUID.randomUUID(),composition(1)).attempt();
            assertThat(cases(s,NOW.plusSeconds(86399)).read(VISITOR,guest.id()).finishedAt()).isEqualTo(NOW);
            assertThatThrownBy(() -> cases(s,NOW.plusSeconds(86400)).read(VISITOR,guest.id())).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(() -> cases(s,NOW.plusSeconds(86400)).create(VISITOR,oldOp,composition(1))).isInstanceOf(Attempt.Unavailable.class);
            var restartedStore=store(db);
            try (var context=new AnnotationConfigApplicationContext()) {
                context.registerBean(ExpiredAttemptCleanup.class,() -> new ExpiredAttemptCleanup(restartedStore,Clock.fixed(NOW.plusSeconds(86400),ZoneOffset.UTC)));
                context.refresh(); context.publishEvent(new ApplicationReadyEvent(new SpringApplication(AttemptHistoryExpiryTests.class),new String[0],context,Duration.ZERO));
            }
            assertThatThrownBy(() -> cases(restartedStore,NOW.plusSeconds(86400)).read(VISITOR,guest.id())).isInstanceOf(Attempt.Unavailable.class);
            assertThat(cases(restartedStore,NOW.plusSeconds(86400)).read(VISITOR,newer.id()).id()).isEqualTo(newer.id());
            assertThat(cases(restartedStore,NOW.plusSeconds(86400)).read(ACCOUNT,account.id()).id()).isEqualTo(account.id());
            assertThat(db.jdbc.queryForObject("select count(*) from me_command where attempt_id=?",Integer.class,guest.id())).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from me_item where attempt_id=?",Integer.class,guest.id())).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from app_user",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from qb_revision",Integer.class)).isEqualTo(1);
            new ExpiredAttemptCleanup(s,Clock.fixed(NOW.plusSeconds(86460),ZoneOffset.UTC)).run();
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt where owner_kind='VISITOR'",Integer.class)).isZero();
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(1);
        }
    }
    @Test void schedulerRegistersFiveMinuteRateAndRunRecoversMissedExpirationAfterDowntime() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var s=store(db); cases(s,NOW).create(VISITOR,UUID.randomUUID(),composition(1));
            try (var context=new AnnotationConfigApplicationContext()) {
                context.registerBean(ScheduledAnnotationBeanPostProcessor.class);
                context.registerBean(ExpiredAttemptCleanup.class,() -> new ExpiredAttemptCleanup(s,Clock.fixed(NOW.plusSeconds(86400+3599),ZoneOffset.UTC)));
                context.refresh();
                var processor=context.getBean(ScheduledAnnotationBeanPostProcessor.class);
                assertThat(processor.getScheduledTasks()).singleElement().satisfies(task -> {
                    assertThat(task.getTask()).isInstanceOf(IntervalTask.class);
                    assertThat(((IntervalTask)task.getTask()).getIntervalDuration()).isEqualTo(Duration.ofMinutes(5));
                    task.getTask().getRunnable().run();
                });
            }
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isZero();
        }
    }
}

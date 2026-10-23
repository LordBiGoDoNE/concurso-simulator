package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import br.com.concursosimulator.questionbank.application.port.EligibleRevisionPool;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class AttemptPersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    JpaAttemptStore store(AttemptDatabase db) { return new JpaAttemptStore(db.entities,db.manager,db.pool); }
    AttemptUseCases cases(AttemptStore store) { return new AttemptUseCases(store,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42)); }
    void seed(AttemptDatabase db) {
        db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());
        for (int i=0;i<5;i++) db.seed("Questão " + i,true);
    }
    @Test void serverClockPrecisionRoundTripsWithoutChangingCapturedDates() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var s=store(db);
            var uc=new AttemptUseCases(s,Clock.fixed(NOW.plusNanos(123456789),ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42));
            var created=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            assertThat(created.createdAt()).isEqualTo(NOW.plusNanos(123456000));
            assertThat(uc.read(ACCOUNT,created.id())).isEqualTo(created);
        }
    }
    @Test void realPoolSelectionUsesRoleTopicDisciplineAndNoReplacement() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var c=br.com.concursosimulator.questionbank.QuestionFixtures.content("Outro assunto");
            var classification=new br.com.concursosimulator.questionbank.domain.QuestionClassification(
                    br.com.concursosimulator.questionbank.domain.Discipline.PORTUGUESE,
                    br.com.concursosimulator.questionbank.domain.Topic.P02,
                    Set.of(br.com.concursosimulator.questionbank.domain.TargetRole.SCHOOL_SECRETARY));
            var other=db.seed(new br.com.concursosimulator.questionbank.domain.QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),classification,c.references(),c.provenance()),true);
            var composition=new Composition(br.com.concursosimulator.questionbank.domain.TargetRole.SCHOOL_SECRETARY,
                    Map.of(br.com.concursosimulator.questionbank.domain.Discipline.PORTUGUESE,new Composition.Portion(1,Set.of(br.com.concursosimulator.questionbank.domain.Topic.P02))));
            var uc=cases(store(db)); var selected=uc.create(ACCOUNT,UUID.randomUUID(),composition).attempt();
            assertThat(selected.items()).singleElement().satisfies(i -> assertThat(i.questionId()).isEqualTo(other.id()));
            var all=uc.create(ACCOUNT,UUID.randomUUID(),composition(5)).attempt();
            assertThat(all.items().stream().map(Attempt.ItemView::questionId).toList()).doesNotHaveDuplicates().doesNotContain(other.id());
            assertThatThrownBy(() -> uc.create(ACCOUNT,UUID.randomUUID(),composition(6))).isInstanceOf(Composition.InsufficientPool.class);
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(2);
        }
    }
    @Test void snapshotsResponsesGradesAndCommandsSurviveFreshRepositoryAndEditorialWithdrawal() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var uc=cases(store(db)); var create=UUID.randomUUID();
            var view=uc.create(ACCOUNT,create,composition(5)).attempt();
            var item=view.items().getFirst(); var answer=UUID.randomUUID();
            var confirmed=uc.answer(ACCOUNT,view.id(),answer,0,item.id(),item.alternatives().getFirst().id());
            for (var q:view.items()) db.decide(q.questionId(),EditorialDecision.Action.WITHDRAW);
            var fresh=cases(store(db));
            assertThat(fresh.read(ACCOUNT,view.id())).isEqualTo(confirmed.attempt());
            assertThat(fresh.create(ACCOUNT,create,composition(5)).attempt()).isEqualTo(view);
            var finish=UUID.randomUUID(); var result=fresh.finish(ACCOUNT,view.id(),finish,1);
            assertThat(result.result().blank()).isEqualTo(4);
            assertThat(fresh.finish(ACCOUNT,view.id(),finish,1)).isEqualTo(new AttemptUseCases.Finished(result.attempt(),result.result(),true));
            assertThat(fresh.answer(ACCOUNT,view.id(),answer,0,item.id(),item.alternatives().getFirst().id()).attempt()).isEqualTo(confirmed.attempt());
            assertThatThrownBy(() -> fresh.finish(ACCOUNT,view.id(),UUID.randomUUID(),2)).isInstanceOf(Attempt.Conflict.class);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(3);
        }
    }
    @Test void concurrentDuplicateCreationsRecoverWinnerAfterFullRollbackAndPayloadMismatchConflicts() throws Exception {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var barrier=new CyclicBarrier(2);
            EligibleRevisionPool pool=() -> { var captured=db.pool.capture(); await(barrier); return captured; };
            var uc=cases(new JpaAttemptStore(db.entities,db.manager,pool)); var op=UUID.randomUUID();
            try (var executor=Executors.newFixedThreadPool(2)) {
                var a=executor.submit(() -> uc.create(ACCOUNT,op,composition(3)));
                var b=executor.submit(() -> uc.create(ACCOUNT,op,composition(3)));
                var first=a.get(20,TimeUnit.SECONDS); var second=b.get(20,TimeUnit.SECONDS);
                assertThat(first.attempt()).isEqualTo(second.attempt());
                assertThat(List.of(first.replayed(),second.replayed())).containsExactlyInAnyOrder(false,true);
            }
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from me_item",Integer.class)).isEqualTo(3);
            assertThatThrownBy(() -> cases(store(db)).create(ACCOUNT,op,composition(2))).isInstanceOf(Attempt.Conflict.class);
        }
    }
    @Test void twoTabsAndDistinctFinishesUseCompareAndSetWithoutOverwritingWinner() throws Exception {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var base=store(db); var uc=cases(base); var view=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var item=view.items().getFirst(); var concurrent=cases(intercept(base,"find",() -> {}));
            var results=race(() -> concurrent.answer(ACCOUNT,view.id(),UUID.randomUUID(),0,item.id(),item.alternatives().getFirst().id()),
                    () -> concurrent.answer(ACCOUNT,view.id(),UUID.randomUUID(),0,item.id(),null));
            assertThat(results.stream().filter(AttemptUseCases.Outcome.class::isInstance).count()).withFailMessage("%s",results).isEqualTo(1);
            assertThat(results.stream().filter(Attempt.Conflict.class::isInstance).count()).withFailMessage("%s",results).isEqualTo(1);
            assertThat(uc.read(ACCOUNT,view.id()).version()).isEqualTo(1);
            var finishing=cases(intercept(base,"find",() -> {}));
            var finishes=race(() -> finishing.finish(ACCOUNT,view.id(),UUID.randomUUID(),1),() -> finishing.finish(ACCOUNT,view.id(),UUID.randomUUID(),1));
            assertThat(finishes.stream().filter(AttemptUseCases.Finished.class::isInstance).count()).isEqualTo(1);
            assertThat(finishes.stream().filter(Attempt.Conflict.class::isInstance).count()).isEqualTo(1);
            assertThat(uc.read(ACCOUNT,view.id()).version()).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(3);
        }
    }
    @Test void concurrentSameFinishOperationReplaysExactlyOneCommittedResult() throws Exception {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var base=store(db); var view=cases(base).create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var uc=cases(intercept(base,"find",() -> {})); var op=UUID.randomUUID();
            var outcomes=race(() -> uc.finish(ACCOUNT,view.id(),op,0),() -> uc.finish(ACCOUNT,view.id(),op,0));
            assertThat(outcomes).allMatch(AttemptUseCases.Finished.class::isInstance);
            var values=outcomes.stream().map(AttemptUseCases.Finished.class::cast).toList();
            assertThat(values.getFirst().result()).isEqualTo(values.getLast().result());
            assertThat(values.stream().filter(AttemptUseCases.Finished::replayed).count()).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(2);
        }
    }
    @Test void withdrawalAfterCapturePreservesSnapshotAndWithdrawalBeforeCaptureMakesCreationAtomicInsufficiency() throws Exception {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var captured=new CountDownLatch(1); var withdrawn=new CountDownLatch(1);
            EligibleRevisionPool pool=() -> { var rows=db.pool.capture(); captured.countDown(); await(withdrawn); return rows; };
            var uc=cases(new JpaAttemptStore(db.entities,db.manager,pool));
            try (var executor=Executors.newSingleThreadExecutor()) {
                var creating=executor.submit(() -> uc.create(ACCOUNT,UUID.randomUUID(),composition(5)));
                assertThat(captured.await(10,TimeUnit.SECONDS)).isTrue();
                var ids=db.jdbc.queryForList("select id from qb_question",UUID.class);
                try { for (var id:ids) db.decide(id,EditorialDecision.Action.WITHDRAW); } finally { withdrawn.countDown(); }
                assertThat(creating.get(20,TimeUnit.SECONDS).attempt().items()).hasSize(5);
            }
            assertThatThrownBy(() -> cases(store(db)).create(ACCOUNT,UUID.randomUUID(),composition(1))).isInstanceOf(Composition.InsufficientPool.class);
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from me_command",Integer.class)).isEqualTo(1);
        }
    }
    @Test void unknownFailuresAndForeignKeysRollbackAttemptItemsAndCommandsAndLostAcknowledgementCanRetry() {
        try (var db=new AttemptDatabase(PG)) {
            seed(db); var base=store(db);
            AttemptStore failing=new AttemptStore() {
                public <T> T transaction(Function<Session,T> work) {
                    return base.transaction(s -> { work.apply(s); throw new IllegalStateException("unknown failure"); });
                }
            };
            assertThatThrownBy(() -> cases(failing).create(ACCOUNT,UUID.randomUUID(),composition(2))).isInstanceOf(IllegalStateException.class).hasMessage("unknown failure");
            var absent=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
            assertThatThrownBy(() -> cases(base).create(absent,UUID.randomUUID(),composition(1))).isNotInstanceOf(Attempt.Conflict.class);
            for (var table:List.of("me_attempt","me_item","me_command")) assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isZero();
            var op=UUID.randomUUID(); cases(base).create(ACCOUNT,op,composition(2)); // Resposta perdida no cliente depois do commit.
            assertThat(cases(store(db)).create(ACCOUNT,op,composition(2)).replayed()).isTrue();
            assertThat(db.jdbc.queryForObject("select count(*) from me_attempt",Integer.class)).isEqualTo(1);
        }
    }
    static AttemptStore intercept(AttemptStore base,String method,Runnable action) {
        var calls=new AtomicInteger(); var barrier=new CyclicBarrier(2);
        return new AttemptStore() {
            public <T> T transaction(Function<Session,T> work) {
                return base.transaction(s -> work.apply((Session)Proxy.newProxyInstance(Session.class.getClassLoader(),new Class<?>[]{Session.class},(p,m,args) -> {
                    Object value;
                    try { value=m.invoke(s,args); } catch (InvocationTargetException e) { throw e.getCause(); }
                    if (m.getName().equals(method) && calls.getAndIncrement()<2) { action.run(); await(barrier); }
                    return value;
                })));
            }
        };
    }
    static List<Object> race(Callable<?> a,Callable<?> b) throws Exception {
        try (var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(() -> outcome(a)); var second=executor.submit(() -> outcome(b));
            return List.of(first.get(25,TimeUnit.SECONDS),second.get(25,TimeUnit.SECONDS));
        }
    }
    static Object outcome(Callable<?> action) { try { return action.call(); } catch (Exception e) { return e; } }
    static void await(CyclicBarrier barrier) { try { barrier.await(10,TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); } }
    static void await(CountDownLatch latch) { try { if (!latch.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("timeout"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); } }
}

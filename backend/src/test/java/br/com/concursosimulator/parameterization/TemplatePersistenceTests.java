package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.*;
import br.com.concursosimulator.parameterization.infrastructure.persistence.*;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import static br.com.concursosimulator.parameterization.ParameterFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class TemplatePersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @Test void registrationApprovalReloadWithdrawalAndImmutableHistoryRoundTripThroughJpa() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);var uc=new TemplateUseCases(db.store);var v=db.approve(d);
            assertThat(uc.register(d,0).replayed()).isTrue();
            var fresh=new JpaTemplateStore(db.entities,db.manager);
            assertThat(fresh.<TemplateVersion>transaction(s -> s.loadForUpdate(d.id()).orElseThrow().eligible().orElseThrow())).isEqualTo(v);
            assertThatThrownBy(db.eligible::capture).isInstanceOf(IllegalStateException.class);
            assertThat(fresh.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).containsExactly(v);
            uc.decide(decision(v,TemplateDecision.Action.WITHDRAW),1,2);
            assertThat(fresh.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).isEmpty();
            uc.register(d,0);assertThat(fresh.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).isEmpty();
            assertThatThrownBy(() -> db.jdbc.update("update pq_version set payload='{}'::jsonb")).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("delete from pq_decision")).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("delete from pq_family")).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_decision",Integer.class)).isEqualTo(3);
        }
    }
    @Test void exactBaseRevisionAndApprovalAreRequiredAndEligibilityDropsOnBaseReplacement() {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.SIMPLE_INTEREST);var v=db.approve(d);var uc=new TemplateUseCases(db.store);
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.WITHDRAW);
            assertThat(db.store.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).isEmpty();
            assertThatThrownBy(() -> uc.decide(decision(v,TemplateDecision.Action.ACTIVATE),1,2)).isInstanceOf(IllegalArgumentException.class);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_decision",Integer.class)).isEqualTo(2);
            db.questions.transaction(s -> {
                var q=s.loadForUpdate(d.baseQuestionId()).orElseThrow();q.append(UUID.randomUUID(),q.latest().content());s.save(q);return null;
            });
            db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.SUBMIT);db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.APPROVE);
            assertThat(db.store.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).isEmpty();
            assertThat(db.store.<TemplateVersion>transaction(s -> s.loadForUpdate(d.id()).orElseThrow().eligible().orElseThrow())).isEqualTo(v);
        }
    }
    @Test void successionAndConcurrentDecisionsNeverSelectTwoVersionsOrLoseHistory() throws Exception {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.DIRECT_PROPORTION);var v1=db.approve(d);var uc=new TemplateUseCases(db.store);
            var v2=uc.register(version(d,2),1).version();uc.decide(decision(v2,TemplateDecision.Action.SUBMIT),1,2);
            var outcomes=race(() -> uc.decide(decision(v2,TemplateDecision.Action.APPROVE),1,3),
                    () -> uc.decide(decision(v1,TemplateDecision.Action.WITHDRAW),1,3));
            assertThat(outcomes.stream().filter(Boolean.class::isInstance).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(TemplateStore.Stale.class::isInstance).count()).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_decision",Integer.class)).isEqualTo(4);
            assertThat(db.store.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).hasSizeLessThanOrEqualTo(1);
        }
    }
    @Test void duplicateRegistrationRecoversOnlyAfterRollbackAndOtherFailuresPropagate() throws Exception {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);var barrier=new CyclicBarrier(2);
            TemplateStore intercepted=new TemplateStore(){public <T>T transaction(Function<Session,T> action){return db.store.transaction(s -> action.apply((Session)Proxy.newProxyInstance(Session.class.getClassLoader(),new Class<?>[]{Session.class},(proxy,method,args) -> {
                try {var result=method.invoke(s,args);if(method.getName().equals("findBaseForUpdate") && ((Optional<?>)result).isEmpty())barrier.await(10,TimeUnit.SECONDS);return result;}
                catch(InvocationTargetException e){throw e.getCause();}
            })));}};
            var cases=new TemplateUseCases(intercepted);var results=race(() -> cases.register(d,0),() -> cases.register(d,0));
            assertThat(results).allMatch(TemplateUseCases.Registered.class::isInstance);
            assertThat(results.stream().map(r -> ((TemplateUseCases.Registered)r).replayed())).containsExactlyInAnyOrder(false,true);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_version",Integer.class)).isEqualTo(1);
            assertThatThrownBy(() -> db.store.transaction(s -> {db.entities.createNativeQuery("insert into pq_family(id,base_question_id,kind) values (:id,:q,'PERCENTAGE')").setParameter("id",UUID.randomUUID()).setParameter("q",UUID.randomUUID()).executeUpdate();return null;}))
                    .isNotInstanceOf(TemplateStore.ConcurrentRegistration.class);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_family",Integer.class)).isEqualTo(1);
        }
    }
    @Test void consumerSnapshotRetainsCapturedEligibilityWhileWithdrawalCommitsInAnotherTransaction() throws Exception {
        snapshotWithdrawal(false);
    }
    @Test void consumerSnapshotAlsoPreservesTheBaseRevisionEligibilityAcrossConcurrentWithdrawal() throws Exception {
        snapshotWithdrawal(true);
    }
    private void snapshotWithdrawal(boolean withdrawBase) throws Exception {
        try(var db=new ParameterDatabase(PG)) {
            var d=db.seed(Family.PERCENTAGE);var v=db.approve(d);var captured=new CountDownLatch(1);var withdrawn=new CountDownLatch(1);
            try(var executor=Executors.newSingleThreadExecutor()) {
                var reader=executor.submit(() -> {
                    var tx=new TransactionTemplate(db.manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                    return tx.execute(status -> {var first=db.eligible.capture();captured.countDown();await(withdrawn);assertThat(db.eligible.capture()).isEqualTo(first);return first;});
                });
                assertThat(captured.await(10,TimeUnit.SECONDS)).isTrue();
                if(withdrawBase)db.bankDecision(d.baseQuestionId(),EditorialDecision.Action.WITHDRAW);
                else new TemplateUseCases(db.store).decide(decision(v,TemplateDecision.Action.WITHDRAW),1,2);
                withdrawn.countDown();
                assertThat(reader.get(20,TimeUnit.SECONDS)).containsExactly(v);
            }
            assertThat(db.store.<List<TemplateVersion>>transaction(s -> db.eligible.capture())).isEmpty();
        }
    }
    private static List<Object> race(Callable<?> a,Callable<?> b) throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Object>>();
            for(var action:List.of(a,b))futures.add(executor.submit(() -> {ready.countDown();await(go);try{return action.call();}catch(Exception e){return e;}}));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();return List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));
        }
    }
    private static void await(CountDownLatch latch){try{if(!latch.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Timeout da fixture");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.*;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static br.com.concursosimulator.mockexam.CompletedEvidenceFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class CompletedEvidenceConsistencyTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    static final TargetRole ROLE=TargetRole.ADMINISTRATIVE_ASSISTANT;
    @Test void additiveIndexRemovesTheWindowSortAndPreservesRowsAndCompatibleReadsAfterReopen() throws Exception {
        try(var db=new AttemptDatabase(PG,"9")) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());
            var other=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());db.jdbc.update("insert into app_user(id) values (?)",other.id());
            var q=question(db);var samples=new ArrayList<Attempt.Snapshot>();
            for(int i=0;i<2400;i++)samples.add(snapshot(i%2==0?ACCOUNT:other,
                    i%4==0?ROLE:TargetRole.SCHOOL_SECRETARY,UUID.randomUUID(),NOW.plusSeconds(i),List.of(q),true));
            insert(db,samples);db.jdbc.execute("analyze me_attempt");var original=rows(db);
            var before=plan(db);var reader=new JpaCompletedAttempts(db.entities,db.manager);
            var expected=reader.recent(ACCOUNT,ROLE,100,NOW.plusSeconds(3000));
            db.migrate("10");db.jdbc.execute("analyze me_attempt");var after=plan(db);
            assertThat(before).contains("Sort");assertThat(after).contains("me_completed_subject_window_idx").doesNotContain("Sort");
            assertThat(rows(db)).isEqualTo(original);assertThat(reader.recent(ACCOUNT,ROLE,100,NOW.plusSeconds(3000))).isEqualTo(expected);
            var logs=Path.of("build/subject-review-plans");Files.createDirectories(logs);
            Files.writeString(logs.resolve("before-v10.txt"),before);Files.writeString(logs.resolve("after-v10.txt"),after);
            db.factory.close();db.open();var fresh=new JpaCompletedAttempts(db.entities,db.manager);
            assertThat(fresh.recent(ACCOUNT,ROLE,100,NOW.plusSeconds(3000))).isEqualTo(expected);
            var store=new JpaAttemptStore(db.entities,db.manager,()->{throw new AssertionError("Leitura não seleciona");});
            var rollback=new AttemptUseCases(store,Clock.fixed(NOW.plusSeconds(3000),ZoneOffset.UTC),UUID::randomUUID,
                    ()->{throw new AssertionError("Leitura não sorteia");},false);
            assertThat(rollback.result(ACCOUNT,expected.attempts().getFirst().id()).blank()).isEqualTo(1);
            assertThat(rows(db)).isEqualTo(original);
        }
    }
    String plan(AttemptDatabase db) {
        return String.join("\n",db.jdbc.queryForList("explain (analyze,buffers) select id from me_attempt where owner_kind='ACCOUNT' "
                +"and owner_id=? and created_at<=? and finished_at is not null and payload->'composition'->>'role'=? order by finished_at desc,id desc limit 101",
                String.class,ACCOUNT.id(),java.sql.Timestamp.from(NOW.plusSeconds(3000)),ROLE.name()));
    }
    @Test void completionDuringAReadOnlyRepeatableReadAppearsOnlyInTheNextWindow() throws Exception {
        try(var db=new AttemptDatabase(PG)) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());var q=question(db);
            var old=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),true);
            insert(db,List.of(old));
            var creation=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,()->new Random());
            var open=creation.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
            var captured=new CountDownLatch(1);var resume=new CountDownLatch(1);
            var isolation=new AtomicReference<String>();var readOnly=new AtomicReference<String>();
            var paused=afterHeaders(db,captured,resume,isolation,readOnly);
            try(var executor=Executors.newSingleThreadExecutor()) {
                var future=executor.submit(()->new JpaCompletedAttempts(paused,db.manager).recent(ACCOUNT,ROLE,1,NOW.plusSeconds(2)));
                assertThat(captured.await(15,TimeUnit.SECONDS)).isTrue();
                try {
                    var uc=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(NOW.plusSeconds(1),ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
                    uc.finish(ACCOUNT,open.id(),UUID.randomUUID(),0);
                } finally { resume.countDown(); }
                var first=future.get(15,TimeUnit.SECONDS);assertThat(first.attempts()).extracting(CompletedAttempt::id).containsExactly(old.id());
                assertThat(first.hasOlder()).isFalse();assertThat(isolation.get()).isEqualTo("repeatable read");assertThat(readOnly.get()).isEqualTo("on");
                var second=new JpaCompletedAttempts(db.entities,db.manager).recent(ACCOUNT,ROLE,1,NOW.plusSeconds(2));
                assertThat(second.attempts()).extracting(CompletedAttempt::id).containsExactly(open.id());assertThat(second.hasOlder()).isTrue();
            } finally { resume.countDown(); }
        }
    }
    @Test void creationAndFinishBeforeTheFirstSelectCannotInvalidateTheCapturedAccessInstant() throws Exception {
        try(var db=new AttemptDatabase(PG)) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());var q=question(db);
            var old=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),true);insert(db,List.of(old));
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            var paused=(EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityManager.class},(proxy,method,args)-> {
                // No SQL (including SHOW) before this latch: the RR snapshot must see both later commits.
                if(method.getName().equals("createNativeQuery") && args[0].toString().startsWith("select id from me_attempt")) {
                    entered.countDown();if(!release.await(15,TimeUnit.SECONDS))throw new AssertionError("Fixture não liberada");
                }
                return invoke(method,db.entities,args);
            });
            try(var executor=Executors.newSingleThreadExecutor()) {
                var first=executor.submit(()->new JpaCompletedAttempts(paused,db.manager).recent(ACCOUNT,ROLE,1,NOW));
                assertThat(entered.await(15,TimeUnit.SECONDS)).isTrue();
                UUID latest=null;List<List<String>> frozen;
                try {
                    // Two later captures fill limit+1: filtering after the limit would lose the old base.
                    for(int i=1;i<=2;i++) {
                        var cases=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(NOW.plusSeconds(i),ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
                        var created=cases.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();cases.finish(ACCOUNT,created.id(),UUID.randomUUID(),0);latest=created.id();
                    }
                    frozen=rows(db);
                } finally {release.countDown();}
                var current=first.get(15,TimeUnit.SECONDS);assertThat(current.attempts()).extracting(CompletedAttempt::id).containsExactly(old.id());assertThat(current.hasOlder()).isFalse();
                var next=new JpaCompletedAttempts(db.entities,db.manager).recent(ACCOUNT,ROLE,1,NOW.plusSeconds(3));
                assertThat(next.attempts()).extracting(CompletedAttempt::id).containsExactly(latest);assertThat(next.hasOlder()).isTrue();
                assertThat(rows(db)).isEqualTo(frozen);
            } finally {release.countDown();}
        }
    }
    @Test void subMicrosecondAccessCutoffCannotRoundUpToAFutureCapture() {
        try(var db=new AttemptDatabase(PG)) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());var q=question(db);
            var old=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),true);insert(db,List.of(old));
            var createdAt=NOW.plusNanos(1000);
            var cases=new AttemptUseCases(new JpaAttemptStore(db.entities,db.manager,db.pool),Clock.fixed(createdAt,ZoneOffset.UTC),UUID::randomUUID,()->new Random(),false);
            var future=cases.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();cases.finish(ACCOUNT,future.id(),UUID.randomUUID(),0);
            var frozen=rows(db);var reader=new JpaCompletedAttempts(db.entities,db.manager);
            // pgjdbc rounds a raw +600ns Timestamp up to +1000ns; that is later than the domain reference.
            var current=reader.recent(ACCOUNT,ROLE,1,NOW.plusNanos(600));
            assertThat(current.attempts()).extracting(CompletedAttempt::id).containsExactly(old.id());assertThat(current.hasOlder()).isFalse();
            var next=reader.recent(ACCOUNT,ROLE,1,createdAt);
            assertThat(next.attempts()).extracting(CompletedAttempt::id).containsExactly(future.id());assertThat(next.hasOlder()).isTrue();
            assertThat(rows(db)).isEqualTo(frozen);
        }
    }
    @Test void tiedUuidOrderingMatchesTheDatabaseAcrossTheSignedLongBoundary() {
        try(var db=new AttemptDatabase(PG)) {
            db.jdbc.update("insert into app_user(id) values (?)",ACCOUNT.id());var q=question(db);
            var low=UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");var high=UUID.fromString("80000000-0000-0000-0000-000000000000");
            insert(db,List.of(snapshot(ACCOUNT,ROLE,low,NOW,List.of(q),true),snapshot(ACCOUNT,ROLE,high,NOW,List.of(q),true)));
            var r=new JpaCompletedAttempts(db.entities,db.manager).recent(ACCOUNT,ROLE,1,NOW);
            assertThat(r.attempts()).extracting(CompletedAttempt::id).containsExactly(high);assertThat(r.hasOlder()).isTrue();
        }
    }
    EntityManager afterHeaders(AttemptDatabase db,CountDownLatch captured,CountDownLatch resume,
            AtomicReference<String> isolation,AtomicReference<String> readOnly) {
        return (EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityManager.class},(proxy,method,args)-> {
            var result=invoke(method,db.entities,args);
            if(method.getName().equals("createNativeQuery") && args[0].toString().startsWith("select id from me_attempt")) {
                var query=(Query)result;
                return Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Query.class},(p,m,a)-> {
                    var value=invoke(m,query,a);
                    if(m.getName().equals("getResultList")) {
                        isolation.set((String)db.entities.createNativeQuery("show transaction_isolation",String.class).getSingleResult());
                        readOnly.set((String)db.entities.createNativeQuery("show transaction_read_only",String.class).getSingleResult());
                        captured.countDown();if(!resume.await(15,TimeUnit.SECONDS))throw new AssertionError("Conclusão concorrente não liberou leitura");
                    }
                    return value==query?p:value;
                });
            }
            return result;
        });
    }
    static Object invoke(Method method,Object target,Object[] args) throws Throwable {
        try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
}

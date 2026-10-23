package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.mockexam.infrastructure.persistence.JpaCompletedAttempts;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static br.com.concursosimulator.mockexam.CompletedEvidenceFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class CompletedAttemptPersistenceTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    static final TargetRole ROLE=TargetRole.ADMINISTRATIVE_ASSISTANT;
    JpaCompletedAttempts reader(AttemptDatabase db) { return new JpaCompletedAttempts(db.entities,db.manager); }
    void account(AttemptDatabase db,AttemptOwner owner) { db.jdbc.update("insert into app_user(id) values (?)",owner.id()); }
    @Test void roleAndCompletionFiltersPrecedeTheLimitAndTheExtraHeaderIsNotDecoded() {
        try(var db=new AttemptDatabase(PG)) {
            account(db,ACCOUNT);var other=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());account(db,other);
            var q=question(db);var selected=new ArrayList<Attempt.Snapshot>();
            for(int i=1;i<=103;i++)selected.add(snapshot(ACCOUNT,ROLE,new UUID(0,i),NOW.plusSeconds(i),List.of(q),true));
            var noise=new ArrayList<Attempt.Snapshot>();
            for(int i=0;i<5;i++) {
                noise.add(snapshot(other,ROLE,UUID.randomUUID(),NOW.plusSeconds(200+i),List.of(q),true));
                noise.add(snapshot(ACCOUNT,TargetRole.SCHOOL_SECRETARY,UUID.randomUUID(),NOW.plusSeconds(200+i),List.of(q),true));
            }
            noise.add(snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW.plusSeconds(300),List.of(q),false));
            insert(db,selected);insert(db,noise);
            // O 101º cabeçalho tem formato inválido: só seu UUID pode ser lido para hasOlder.
            db.jdbc.execute("alter table me_attempt disable trigger user");
            db.jdbc.update("update me_attempt set payload=jsonb_set(payload,'{format}','99') where id=?",new UUID(0,3));
            db.jdbc.execute("alter table me_attempt enable trigger user");
            var before=rows(db);var r=reader(db).recent(ACCOUNT,ROLE,100,NOW.plusSeconds(400));
            assertThat(r.hasOlder()).isTrue();assertThat(r.attempts()).hasSize(100);
            assertThat(r.attempts().getFirst().id()).isEqualTo(new UUID(0,103));assertThat(r.attempts().getLast().id()).isEqualTo(new UUID(0,4));
            assertThat(r.attempts()).allSatisfy(a -> {assertThat(a.owner()).isEqualTo(ACCOUNT);assertThat(a.role()).isEqualTo(ROLE);});
            assertThatThrownBy(() -> reader(db).one(ACCOUNT,new UUID(0,3),NOW.plusSeconds(400))).isInstanceOf(IllegalStateException.class);
            assertThat(rows(db)).isEqualTo(before);
        }
    }
    @Test void ownershipVisitorExpiryOpenStateAndInvalidLimitsDoNotLeakEvidence() {
        try(var db=new AttemptDatabase(PG)) {
            account(db,ACCOUNT);var q=question(db);var finished=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),true);
            var open=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),false);
            var visitor=snapshot(VISITOR,ROLE,UUID.randomUUID(),NOW,List.of(q),true);insert(db,List.of(finished,open,visitor));
            var s=reader(db);assertThat(s.one(ACCOUNT,finished.id(),NOW).items()).hasSize(1);
            assertThat(s.one(VISITOR,visitor.id(),NOW).owner()).isEqualTo(VISITOR);
            assertThatThrownBy(() -> s.one(VISITOR,finished.id(),NOW)).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(() -> s.one(ACCOUNT,open.id(),NOW)).isInstanceOf(Attempt.Conflict.class);
            assertThatThrownBy(() -> s.one(VISITOR,visitor.id(),NOW.plus(Duration.ofHours(24)))).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(() -> s.one(ACCOUNT,UUID.randomUUID(),NOW)).isInstanceOf(Attempt.Unavailable.class);
            assertThatThrownBy(() -> s.recent(VISITOR,ROLE,100,NOW)).isInstanceOf(Attempt.Unavailable.class);
            for(int limit:List.of(0,101))assertThatThrownBy(() -> s.recent(ACCOUNT,ROLE,limit,NOW)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void selectedCorruptionFailsTheWholeWindowWithoutRepairingOrSkippingRows() {
        try(var db=new AttemptDatabase(PG)) {
            account(db,ACCOUNT);var q=question(db);
            var a=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW,List.of(q),true);
            var b=snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW.plusSeconds(1),List.of(q),true);insert(db,List.of(a,b));
            db.jdbc.execute("alter table me_item disable trigger user");
            db.jdbc.update("update me_item set payload=jsonb_set(payload,'{question,content,statement,text}','\"PRIVATE_BAD_CONTENT\"') where attempt_id=?",a.id());
            db.jdbc.execute("alter table me_item enable trigger user");var before=rows(db);
            var uc=new ReviewUseCases(reader(db),Clock.fixed(NOW.plusSeconds(2),ZoneOffset.UTC));
            assertThatThrownBy(() -> uc.overview(ACCOUNT,ROLE)).isInstanceOf(IllegalStateException.class);
            assertThat(rows(db)).isEqualTo(before);
        }
    }
    @Test void tenThousandItemsUseFourSelectsAndNeverWriteTheStoredEvidence() {
        try(var db=new AttemptDatabase(PG)) {
            account(db,ACCOUNT);var pool=new ArrayList<AttemptQuestion>();for(int i=0;i<100;i++)pool.add(question(db));
            var snapshots=new ArrayList<Attempt.Snapshot>();
            for(int i=0;i<101;i++)snapshots.add(snapshot(ACCOUNT,ROLE,UUID.randomUUID(),NOW.plusSeconds(i),pool,true));
            insert(db,snapshots);var before=rows(db);
            var stats=db.factory.unwrap(SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
            var uc=new ReviewUseCases(reader(db),Clock.fixed(NOW.plusSeconds(200),ZoneOffset.UTC));var result=uc.overview(ACCOUNT,ROLE);
            assertThat(result.window().attemptCount()).isEqualTo(100);assertThat(result.window().hasOlder()).isTrue();
            assertThat(result.summary().subjects().stream().mapToInt(m -> m.exposures()).sum()).isEqualTo(10_000);
            assertThat(result.summary().subjects().stream().mapToInt(m -> m.assessed()).sum()).isEqualTo(100);
            assertThat(stats.getPrepareStatementCount()).isEqualTo(4);
            assertThat(stats.getEntityInsertCount()+stats.getEntityUpdateCount()+stats.getEntityDeleteCount()).isZero();
            assertThat(rows(db)).isEqualTo(before);
        }
    }
}

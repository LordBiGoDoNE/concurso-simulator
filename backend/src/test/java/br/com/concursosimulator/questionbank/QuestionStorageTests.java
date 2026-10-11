package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.application.DecideQuestionUseCase;
import br.com.concursosimulator.questionbank.application.port.StaleQuestionException;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.*;
import static br.com.concursosimulator.questionbank.QuestionFixtures.*;
import static br.com.concursosimulator.questionbank.QuestionEditorialTests.decision;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class QuestionStorageTests {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18.6");
    QuestionDatabase db;
    @BeforeEach void open() { db=new QuestionDatabase(POSTGRES); }
    @AfterEach void close() { db.close(); }
    Question create(String key) {
        var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("fixture",key),UUID.randomUUID(),content("Questão sintética "+key));
        db.store.transaction(s -> { s.insert(q); return null; }); return q;
    }
    Question load(UUID id) { return db.store.transaction(s->s.loadForUpdate(id).orElseThrow()); }
    @Test void roundTripAndAtomicReplacementPreserveExactContentHistoryAndEligibleClassification() {
        var q=create("roundtrip"); var first=q.latest(); var useCase=new DecideQuestionUseCase(db.store);
        useCase.execute(decision(q,first.id(),SUBMIT),null); useCase.execute(decision(q,first.id(),APPROVE),null);
        var next=db.store.transaction(s->{var x=s.loadForUpdate(q.id()).orElseThrow();var r=x.append(UUID.randomUUID(),content("Outra versão πr²"));s.save(x);return r;});
        var read=load(q.id()); assertThat(read.revisions()).containsExactly(first,next); assertThat(read.eligible()).contains(first);
        useCase.execute(decision(read,next.id(),SUBMIT),first.id()); useCase.execute(decision(read,next.id(),APPROVE),first.id());
        read=load(q.id()); assertThat(read.eligible()).contains(next); assertThat(read.status(first.id())).isEqualTo(Question.Status.APPROVED);
        assertThat(read.revision(first.id()).content()).isEqualTo(first.content());
        useCase.execute(decision(read,next.id(),WITHDRAW),next.id());
        assertThat(load(q.id()).eligible()).isEmpty(); assertThat(load(q.id()).decisions()).hasSize(5);
    }
    @Test void lateFailureRollsBackEveryAggregateAndEditorialChangeInTheUnit() {
        var a=create("a");var b=create("b");
        var useCase=new DecideQuestionUseCase(db.store);useCase.execute(decision(a,a.latest().id(),SUBMIT),null);
        assertThatThrownBy(()->db.store.transaction(s->{
            var first=s.loadForUpdate(a.id()).orElseThrow();first.decide(decision(first,first.latest().id(),APPROVE));s.save(first);
            var second=s.loadForUpdate(b.id()).orElseThrow();second.decide(decision(second,second.latest().id(),APPROVE));s.save(second);return null;
        })).isInstanceOf(IllegalStateException.class);
        assertThat(load(a.id()).eligible()).isEmpty();assertThat(load(a.id()).status(a.latest().id())).isEqualTo(Question.Status.IN_REVIEW);
        assertThat(load(b.id()).decisions()).isEmpty();assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_decision WHERE action='APPROVE'",Integer.class)).isZero();
    }
    @Test void concurrentApprovalsHaveOneWinnerAndRejectTheStaleExpectedPointer() throws Exception {
        var q=create("concurrent");
        var questionId=q.id();
        q=db.store.transaction(s->{var x=s.loadForUpdate(questionId).orElseThrow();x.append(UUID.randomUUID(),content("Nova"));s.save(x);return x;});
        var useCase=new DecideQuestionUseCase(db.store);
        for(var r:q.revisions())useCase.execute(decision(q,r.id(),SUBMIT),null);
        var snapshot=q;var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=snapshot.revisions().stream().map(r->executor.submit(()->{
                assertThat(start.await(5,TimeUnit.SECONDS)).isTrue();
                try { useCase.execute(decision(snapshot,r.id(),APPROVE),null);return true; }
                catch(StaleQuestionException expected){return false;}
            })).toList();start.countDown();
            assertThat(List.of(futures.get(0).get(10,TimeUnit.SECONDS),futures.get(1).get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(load(q.id()).eligible()).isPresent();
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM qb_decision WHERE action='APPROVE'",Integer.class)).isEqualTo(1);
    }
    @Test void storeReturnsOnlyCommittedDataAndCannotLeakAUsableTransactionSession() {
        var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("fixture","ambient"),UUID.randomUUID(),content("Fixture"));
        new TransactionTemplate(db.manager).execute(status->{db.store.transaction(s->{s.insert(q);return null;});status.setRollbackOnly();return null;});
        assertThat(load(q.id()).latest().content()).isEqualTo(q.latest().content());
        var escaped=db.store.transaction(s->s);
        assertThatThrownBy(()->escaped.loadForUpdate(q.id())).isInstanceOf(IllegalStateException.class);
    }
}

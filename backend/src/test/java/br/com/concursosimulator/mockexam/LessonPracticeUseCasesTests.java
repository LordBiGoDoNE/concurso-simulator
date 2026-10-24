package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

class LessonPracticeUseCasesTests {
    static AttemptUseCases practice(AttemptStore store,Instant now) {
        return new AttemptUseCases(store,Clock.fixed(now,ZoneOffset.UTC),UUID::randomUUID,()->new Random(37),false,QuestionPurpose.PRATICA_AULA);
    }
    static AttemptQuestion practiceQuestion(int index) {
        var q=question(index);
        return new AttemptQuestion(q.questionId(),q.revisionId(),q.fingerprint(),q.content(),QuestionSource.fixed(),QuestionPurpose.PRATICA_AULA);
    }
    static AttemptUseCasesTests.Memory store(int practiceCount) {
        var store=new AttemptUseCasesTests.Memory();var pool=new ArrayList<>(pool(8));
        for(int i=0;i<practiceCount;i++) pool.add(practiceQuestion(100+i));
        store.pool=List.copyOf(pool);return store;
    }
    @Test void disjointCollectionsAndLimitedPracticeAreServerControlledAndReplayDoesNotReselect() {
        var s=store(7);var practice=practice(s,NOW);var exam=new AttemptUseCasesTests().cases(s);var command=UUID.randomUUID();
        var available=practice.practiceAvailability(TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01);
        assertThat(available.available()).isEqualTo(7);assertThat(available.maximum()).isEqualTo(5);
        var started=practice.createPractice(ACCOUNT,command,TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01);
        assertThat(started.attempt().items()).hasSize(5);
        assertThat(practice.createPractice(ACCOUNT,command,TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt()).isEqualTo(started.attempt());
        var mock=exam.create(ACCOUNT,UUID.randomUUID(),composition(8)).attempt();
        assertThat(mock.items().stream().map(Attempt.ItemView::questionId)).doesNotContainAnyElementsOf(started.attempt().items().stream().map(Attempt.ItemView::questionId).toList());
        assertThatThrownBy(()->exam.create(ACCOUNT,command,composition(1))).isInstanceOf(Attempt.Conflict.class);
        assertThatThrownBy(()->exam.read(ACCOUNT,started.attempt().id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(()->practice.read(ACCOUNT,mock.id())).isInstanceOf(Attempt.Unavailable.class);
    }
    @Test void emptyPracticeDoesNotBorrowMockQuestionsOrPersistPartialState() {
        var s=store(0);var p=practice(s,NOW);
        assertThatThrownBy(()->p.createPractice(ACCOUNT,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01)).isInstanceOf(Composition.InsufficientPool.class);
        assertThat(s.attempts).isEmpty();assertThat(s.commands).isEmpty();
    }
    @Test void correctionOnlyAfterFinishAndItsRetryCannotTransitionTwice() {
        var s=store(2);var p=practice(s,NOW);var v=p.createPractice(ACCOUNT,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt();
        assertThat(v.items()).hasSize(2);
        assertThatThrownBy(()->p.result(ACCOUNT,v.id())).isInstanceOf(Attempt.Conflict.class);
        var item=v.items().getFirst();p.answer(ACCOUNT,v.id(),UUID.randomUUID(),0,item.id(),item.alternatives().getFirst().id());
        var op=UUID.randomUUID();var result=p.finish(ACCOUNT,v.id(),op,1);
        assertThat(result.result().total()).isEqualTo(2);assertThat(result.result().blank()).isEqualTo(1);
        assertThat(p.finish(ACCOUNT,v.id(),op,1).replayed()).isTrue();assertThat(p.read(ACCOUNT,v.id()).version()).isEqualTo(2);
        var snapshot=s.attempts.get(v.id());assertThat(snapshot.purpose()).isEqualTo(QuestionPurpose.PRATICA_AULA);assertThat(snapshot.format()).isEqualTo(3);
        assertThatThrownBy(()->Attempt.restore(snapshot).evidence(ACCOUNT,NOW)).isInstanceOf(Attempt.Unavailable.class);
    }
    @Test void historyFilteringHappensBeforePaginationAndVisitorExpiryAndOwnershipRemainEnforced() {
        var s=store(1);var exam=new AttemptUseCasesTests().cases(s);
        var old=exam.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
        var p=practice(s,NOW.plusSeconds(1));p.createPractice(ACCOUNT,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01);
        assertThat(exam.history(ACCOUNT,new AttemptStore.HistoryQuery(0,1,null))).singleElement().satisfies(e->assertThat(e.id()).isEqualTo(old.id()));
        assertThat(p.history(ACCOUNT,new AttemptStore.HistoryQuery(0,1,null,QuestionPurpose.PRATICA_AULA))).singleElement().satisfies(e->assertThat(e.topicId()).isEqualTo("P01"));
        var guest=p.createPractice(VISITOR,UUID.randomUUID(),TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01).attempt();
        assertThatThrownBy(()->p.read(ACCOUNT,guest.id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(()->practice(s,NOW.plusSeconds(86401)).read(VISITOR,guest.id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(()->p.history(VISITOR,new AttemptStore.HistoryQuery(0,1,null,QuestionPurpose.PRATICA_AULA))).isInstanceOf(Attempt.Unavailable.class);
    }
    @Test void domainRejectsPuttingAQuestionFromTheOtherCollectionIntoPractice() {
        var q=question(1);var item=new Attempt.Item(UUID.randomUUID(),q,q.content().alternatives().stream().map(a->a.id()).toList(),Map.of());
        var c=new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,Map.of(Discipline.PORTUGUESE,new Composition.Portion(1,Set.of(Topic.P01))));
        assertThatThrownBy(()->Attempt.startPractice(UUID.randomUUID(),ACCOUNT,c,Topic.P01,NOW,List.of(item))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new QuestionOrigin("legacy-v3","P01-Q01").purpose()).isEqualTo(QuestionPurpose.SIMULADO);
        assertThat(new QuestionOrigin(QuestionPurpose.PRACTICE_NAMESPACE,"P01-P01").purpose()).isEqualTo(QuestionPurpose.PRATICA_AULA);
    }
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static br.com.concursosimulator.mockexam.LessonPracticeUseCasesTests.*;
import static org.assertj.core.api.Assertions.*;

class TopicPracticeDomainTests {
    static AttemptQuestion classified(int index,TargetRole role,Topic topic) {
        var q=practiceQuestion(index);var c=q.content();
        var content=new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                new QuestionClassification(topic.discipline(),topic,Set.of(role)),c.references(),c.provenance());
        return new AttemptQuestion(q.questionId(),q.revisionId(),content.fingerprint(),content,QuestionSource.fixed(),QuestionPurpose.PRATICA_AULA);
    }
    static Attempt.Item item(AttemptQuestion q) {
        return new Attempt.Item(UUID.randomUUID(),q,q.content().alternatives().stream().map(QuestionContent.Alternative::id).toList(),Map.of());
    }
    @Test void differentEditorialRolesParticipateInOnePoolAndPracticeNeverAcquiresARole() {
        var s=store(0);s.pool=List.of(classified(1,TargetRole.ADMINISTRATIVE_ASSISTANT,Topic.P01),classified(2,TargetRole.SCHOOL_SECRETARY,Topic.P01),classified(3,TargetRole.SCHOOL_SECRETARY,Topic.P02),question(4));
        var p=practice(s,NOW);assertThat(p.practiceAvailability(Topic.P01).available()).isEqualTo(2);
        var view=p.createPractice(ACCOUNT,UUID.randomUUID(),Topic.P01).attempt();
        assertThat(view.items()).hasSize(2);assertThat(view.role()).isNull();assertThat(view.practiceTopic()).isEqualTo(Topic.P01);
        assertThat(s.attempts.get(view.id()).composition()).isNull();
        assertThat(view.items().stream().flatMap(i->i.classification().roles().stream()).toList()).containsExactlyInAnyOrder(TargetRole.ADMINISTRATIVE_ASSISTANT,TargetRole.SCHOOL_SECRETARY);
    }
    @Test void everySmallPoolUsesAllDistinctBasesAndLargePoolKeepsTheExistingMaximum() {
        for(int count:new int[]{1,5,7,24,25,30}) {
            var s=store(count);var p=practice(s,NOW);var op=UUID.randomUUID();
            var view=p.createPractice(ACCOUNT,op,Topic.P01).attempt();assertThat(view.items()).hasSize(Math.min(count,5));
            assertThat(view.items().stream().map(Attempt.ItemView::questionId).distinct().count()).isEqualTo(Math.min(count,5));
            s.pool=List.of();assertThat(p.createPractice(ACCOUNT,op,Topic.P01).attempt()).isEqualTo(view);
            assertThatThrownBy(()->p.createPractice(ACCOUNT,op,Topic.P02)).isInstanceOf(Attempt.Conflict.class);
        }
    }
    @Test void eachNewPracticeSamplesFiveFromTheWholeTwentyFiveBasePoolAndReplayKeepsTheSameDraw() {
        var s=store(25);var rng=new Random(20261111);
        var p=new br.com.concursosimulator.mockexam.application.AttemptUseCases(s,
                java.time.Clock.fixed(NOW,java.time.ZoneOffset.UTC),UUID::randomUUID,()->rng,false,QuestionPurpose.PRATICA_AULA);
        var command=UUID.randomUUID();var first=p.createPractice(ACCOUNT,command,Topic.P01).attempt();
        assertThat(p.practiceAvailability(Topic.P01).available()).isEqualTo(25);
        assertThat(p.practiceAvailability(Topic.P01).maximum()).isEqualTo(5);
        assertThat(p.createPractice(ACCOUNT,command,Topic.P01).attempt()).isEqualTo(first);
        var second=p.createPractice(ACCOUNT,UUID.randomUUID(),Topic.P01).attempt();
        var a=first.items().stream().map(Attempt.ItemView::questionId).collect(java.util.stream.Collectors.toSet());
        var b=second.items().stream().map(Attempt.ItemView::questionId).collect(java.util.stream.Collectors.toSet());
        assertThat(a).hasSize(5);assertThat(b).hasSize(5).isNotEqualTo(a);
        assertThat(s.pool.stream().filter(q->q.purpose()==QuestionPurpose.PRATICA_AULA).map(AttemptQuestion::questionId).toList()).containsAll(a).containsAll(b);
    }
    @Test void formatFourRejectsHiddenCompositionOtherModesTopicsDuplicatesAndInvalidAnswers() {
        var items=List.of(item(practiceQuestion(1)));var a=Attempt.startPractice(UUID.randomUUID(),ACCOUNT,Topic.P01,NOW,items);var s=a.snapshot();
        assertThatThrownBy(()->Attempt.restore(new Attempt.Snapshot(s.id(),s.owner(),composition(1),NOW,null,s.policy(),items,Map.of(),0,4,s.purpose(),Topic.P01))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->Attempt.restore(new Attempt.Snapshot(s.id(),s.owner(),null,NOW,null,s.policy(),items,Map.of(),0,4,QuestionPurpose.SIMULADO,null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->Attempt.startPractice(UUID.randomUUID(),ACCOUNT,Topic.P02,NOW,items)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->Attempt.startPractice(UUID.randomUUID(),ACCOUNT,Topic.P01,NOW,List.of(items.getFirst(),items.getFirst()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->Attempt.startPractice(UUID.randomUUID(),ACCOUNT,Topic.P01,NOW,List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->a.answer(ACCOUNT,NOW,0,items.getFirst().id(),UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
        var oversized=java.util.stream.IntStream.range(0,6).mapToObj(i->item(practiceQuestion(i))).toList();
        assertThatThrownBy(()->Attempt.startPractice(UUID.randomUUID(),ACCOUNT,Topic.P01,NOW,oversized)).isInstanceOf(IllegalArgumentException.class);
        var historical=java.util.stream.IntStream.range(0,25).mapToObj(i->item(practiceQuestion(i))).toList();
        assertThat(Attempt.restore(new Attempt.Snapshot(UUID.randomUUID(),ACCOUNT,null,NOW,null,Attempt.POLICY,historical,Map.of(),0,4,QuestionPurpose.PRATICA_AULA,Topic.P01)).snapshot().items()).hasSize(25);
    }
}

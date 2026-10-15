package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.QuestionFixtures;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

class AttemptDomainTests {
    @Test void compositionValidatesCriteriaAndSelectsWithoutReplacementDeterministically() {
        for (int n : new int[]{0,-1,101}) assertThatIllegalArgumentException().isThrownBy(() -> composition(n));
        assertThatIllegalArgumentException().isThrownBy(() -> new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,
                Map.of(Discipline.PORTUGUESE,new Composition.Portion(2,Set.of(Topic.M01)))));
        var criteria = composition(5); var pool = pool(10);
        var selected = criteria.select(pool,new Random(31));
        assertThat(selected).hasSize(5).doesNotHaveDuplicates();
        assertThat(criteria.select(pool,new Random(31))).isEqualTo(selected);
        assertThat(criteria.select(pool,new Random(32))).isNotEqualTo(selected);
        assertThatThrownBy(() -> composition(11).select(pool,new Random(1)))
                .isInstanceOfSatisfying(Composition.InsufficientPool.class,e -> assertThat(e.available()).containsEntry(Discipline.PORTUGUESE,10));
        var duplicate = new ArrayList<>(pool); duplicate.add(pool.getFirst());
        assertThatIllegalStateException().isThrownBy(() -> criteria.select(duplicate,new Random()));
    }

    @Test void selectionUsesRoleDisciplineAndUnionOfTopicsWithoutMutatingPool() {
        var original = question(0); var c = original.content();
        var math = new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                new QuestionClassification(Discipline.MATHEMATICS_LOGIC,Topic.M01,Set.of(TargetRole.SCHOOL_SECRETARY)),c.references(),c.provenance());
        var question = new AttemptQuestion(UUID.randomUUID(),UUID.randomUUID(),math.fingerprint(),math);
        var criteria = new Composition(TargetRole.SCHOOL_SECRETARY,Map.of(
                Discipline.MATHEMATICS_LOGIC,new Composition.Portion(1,Set.of(Topic.M01,Topic.M02)),
                Discipline.PORTUGUESE,new Composition.Portion(1,Set.of(Topic.P01))));
        var pool = List.of(original,question);
        assertThat(criteria.select(pool,new Random())).containsExactlyInAnyOrder(original,question);
        assertThat(pool).containsExactly(original,question);
        assertThat(composition(1).accepts(question)).isFalse();
    }

    @Test void snapshotsKeepVersionsOrdersContentAndOwnImmutableResponses() {
        var a = attempt(ACCOUNT,1); var snapshot = a.snapshot(); var item = snapshot.items().getFirst();
        assertThatThrownBy(() -> snapshot.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        a.answer(ACCOUNT,NOW,0,item.id(),QuestionFixtures.SECOND);
        assertThat(snapshot.answers()).isEmpty();
        assertThat(a.snapshot().items()).isEqualTo(snapshot.items());
        assertThat(a.view(ACCOUNT,NOW).items().getFirst().alternatives()).extracting(QuestionContent.Alternative::id)
                .containsExactly(QuestionFixtures.SECOND,QuestionFixtures.FIRST);
        var restored = Attempt.restore(a.snapshot());
        assertThat(restored.view(ACCOUNT,NOW)).isEqualTo(a.view(ACCOUNT,NOW));
        restored.answer(ACCOUNT,NOW,1,item.id(),null);
        assertThat(restored.snapshot().answers()).isEmpty();
        assertThat(a.snapshot().answers()).hasSize(1);
        assertThatIllegalArgumentException().isThrownBy(() -> restored.answer(ACCOUNT,NOW,2,item.id(),UUID.randomUUID()));
        assertThatIllegalArgumentException().isThrownBy(() -> restored.answer(ACCOUNT,NOW,2,UUID.randomUUID(),null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Attempt.Item(UUID.randomUUID(),item.question(),
                List.of(QuestionFixtures.FIRST,QuestionFixtures.FIRST),Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new AttemptQuestion(item.question().questionId(),UUID.randomUUID(),"0".repeat(64),item.question().content()));
    }

    @Test void gradeUsesCapturedCorrectIdsNotLettersAndCannotChangeAfterFinishing() {
        var a = attempt(ACCOUNT,5); long version = 0;
        for (var item : a.snapshot().items().subList(0,3)) a.answer(ACCOUNT,NOW,version++,item.id(),QuestionFixtures.FIRST);
        a.answer(ACCOUNT,NOW,version++,a.snapshot().items().get(3).id(),QuestionFixtures.SECOND);
        assertThatThrownBy(() -> a.result(ACCOUNT,NOW)).isInstanceOf(Attempt.Conflict.class);
        a.finish(ACCOUNT,NOW.plusSeconds(1),version);
        var result = a.result(ACCOUNT,NOW.plusSeconds(2));
        assertThat(result.total()).isEqualTo(5); assertThat(result.correct()).isEqualTo(3);
        assertThat(result.wrong()).isEqualTo(1); assertThat(result.blank()).isEqualTo(1);
        assertThat(result.percentage()).isEqualByComparingTo("60.00");
        assertThat(result.items()).allSatisfy(i -> assertThat(i.correctAlternativeId()).isEqualTo(QuestionFixtures.FIRST));
        assertThatThrownBy(() -> a.finish(ACCOUNT,NOW.plusSeconds(3),5)).isInstanceOf(Attempt.Conflict.class);
        assertThatThrownBy(() -> a.answer(ACCOUNT,NOW.plusSeconds(3),5,a.snapshot().items().getFirst().id(),null)).isInstanceOf(Attempt.Conflict.class);
        assertThat(a.result(ACCOUNT,NOW.plusSeconds(3))).isEqualTo(result);
    }

    @Test void staleVersionWrongOwnerAndExpiredVisitorCannotReadOrChangeState() {
        var a = attempt(VISITOR,1); var item = a.snapshot().items().getFirst();
        assertThat(a.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThatThrownBy(() -> a.answer(VISITOR,NOW,1,item.id(),null)).isInstanceOf(Attempt.Conflict.class);
        assertThatThrownBy(() -> a.answer(ACCOUNT,NOW,0,item.id(),null)).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> a.view(VISITOR,a.expiresAt())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> a.finish(VISITOR,a.expiresAt(),0)).isInstanceOf(Attempt.Unavailable.class);
        assertThat(a.snapshot().version()).isZero();
        assertThat(attempt(ACCOUNT,1).expiresAt()).isNull();
    }

    @Test void activeProjectionHasNoCorrectiveOrProvenanceFieldsAndRoundingIsExplicit() {
        var a = attempt(ACCOUNT,3); var json = JsonMapper.builder().build();
        var body = json.writeValueAsString(a.view(ACCOUNT,NOW));
        assertThat(body).doesNotContain("correctAlternativeId","resolution","provenance","answerPath","Resolução sintética");
        a.answer(ACCOUNT,NOW,0,a.snapshot().items().getFirst().id(),QuestionFixtures.FIRST);
        a.finish(ACCOUNT,NOW,1);
        assertThat(a.result(ACCOUNT,NOW).percentage()).isEqualByComparingTo("33.33");
        assertThat(json.writeValueAsString(a.result(ACCOUNT,NOW))).contains("correctAlternativeId","resolution");
    }
}

package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.UUID;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static br.com.concursosimulator.mockexam.domain.CompletedAttempt.Outcome.*;
import static org.assertj.core.api.Assertions.*;

class CompletedAttemptTests {
    @Test void legacyFixedAndParameterizedSnapshotsProduceTheSameMinimalEvidence() {
        var old=attempt(ACCOUNT,1);old.finish(ACCOUNT,NOW,0);var s=old.snapshot();
        var legacy=new Attempt.Snapshot(s.id(),s.owner(),s.composition(),s.createdAt(),s.finishedAt(),s.policy(),s.items(),s.answers(),s.version(),1);
        assertThat(Attempt.restore(legacy).evidence(ACCOUNT,NOW)).isEqualTo(old.evidence(ACCOUNT,NOW));
        var family=br.com.concursosimulator.parameterization.domain.Family.PERCENTAGE;
        var base=ParameterizedAttemptTests.base(family);
        for(boolean generated:java.util.List.of(false,true)) {
            var questions=generated?AttemptMaterializer.materialize(java.util.List.of(base),
                    java.util.List.of(ParameterizedAttemptTests.template(base,family)),new java.util.Random(4),UUID::randomUUID):java.util.List.of(base);
            var q=questions.getFirst();var capture=q.source().capture();
            var item=new Attempt.Item(UUID.randomUUID(),q,q.content().alternatives().stream().map(a->a.id()).toList(),
                    capture==null?java.util.Map.of():capture.parameterStrings());
            var a=Attempt.start(UUID.randomUUID(),ACCOUNT,ParameterizedAttemptTests.math(1,generated),NOW,java.util.List.of(item));
            a.answer(ACCOUNT,NOW,0,item.id(),q.content().correctAlternativeId());a.finish(ACCOUNT,NOW,1);
            assertThat(a.evidence(ACCOUNT,NOW).items()).containsExactly(new CompletedAttempt.Item(base.questionId(),base.content().classification(),CORRECT));
        }
    }
    @Test void evidenceAndResultShareCorrectWrongBlankWithoutExposingTheAnswer() {
        var a=attempt(ACCOUNT,3);var first=a.snapshot().items().get(0);var second=a.snapshot().items().get(1);
        a.answer(ACCOUNT,NOW,0,first.id(),first.question().content().correctAlternativeId());
        a.answer(ACCOUNT,NOW,1,second.id(),second.optionOrder().stream().filter(id -> !id.equals(second.question().content().correctAlternativeId())).findFirst().orElseThrow());
        a.finish(ACCOUNT,NOW.plusSeconds(1),2);
        var evidence=a.evidence(ACCOUNT,NOW.plusSeconds(2));var result=a.result(ACCOUNT,NOW.plusSeconds(2));
        assertThat(evidence.items()).extracting(CompletedAttempt.Item::outcome).containsExactly(CORRECT,WRONG,BLANK);
        assertThat(result.correct()).isEqualTo(1);assertThat(result.wrong()).isEqualTo(1);assertThat(result.blank()).isEqualTo(1);
        assertThat(evidence.owner()).isEqualTo(ACCOUNT);assertThat(evidence.role()).isEqualTo(a.snapshot().composition().role());
        assertThatThrownBy(() -> evidence.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void ownershipCompletionAndVisitorExpiryApplyBeforeEvidence() {
        var a=attempt(VISITOR,1);
        assertThatThrownBy(() -> a.evidence(VISITOR,NOW)).isInstanceOf(Attempt.Conflict.class);
        assertThatThrownBy(() -> a.evidence(ACCOUNT,NOW)).isInstanceOf(Attempt.Unavailable.class);
        a.finish(VISITOR,NOW.plusSeconds(1),0);
        assertThat(a.evidence(VISITOR,NOW.plusSeconds(2)).items()).hasSize(1);
        assertThatThrownBy(() -> a.evidence(VISITOR,NOW.plus(Duration.ofHours(24)))).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> a.evidence(new AttemptOwner(AttemptOwner.Kind.VISITOR,UUID.randomUUID()),NOW.plusSeconds(2)))
                .isInstanceOf(Attempt.Unavailable.class);
    }
}

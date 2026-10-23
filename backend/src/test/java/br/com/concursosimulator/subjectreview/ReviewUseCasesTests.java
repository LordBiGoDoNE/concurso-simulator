package br.com.concursosimulator.subjectreview;

import br.com.concursosimulator.mockexam.application.CompletedAttemptReader;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.subjectreview.ReviewTestData.*;
import static br.com.concursosimulator.mockexam.domain.CompletedAttempt.Outcome.*;
import static org.assertj.core.api.Assertions.*;

class ReviewUseCasesTests {
    static final class Source implements CompletedAttemptReader {
        Window window=new Window(List.of(),false);CompletedAttempt single;RuntimeException failure;int calls;
        @Override public Window recent(AttemptOwner actor,TargetRole role,int limit,Instant now) {
            calls++;assertThat(actor).isEqualTo(ACCOUNT);assertThat(role).isEqualTo(ROLE);
            assertThat(limit).isEqualTo(100);assertThat(now).isEqualTo(NOW);
            if(failure!=null)throw failure;return window;
        }
        @Override public CompletedAttempt one(AttemptOwner actor,UUID id,Instant now) {
            calls++;if(failure!=null)throw failure;return single;
        }
    }
    ReviewUseCases use(Source source) { return new ReviewUseCases(source,Clock.fixed(NOW,ZoneOffset.UTC)); }
    @Test void overviewHasExplicitWindowAndNoDataWithoutInventedZeroPercentage() {
        var source=new Source();var r=use(source).overview(ACCOUNT,ROLE);
        assertThat(r.window()).isEqualTo(new ReviewUseCases.Window(100,0,false,null,null));
        assertThat(r.summary().subjects()).hasSize(51).allSatisfy(m -> assertThat(m.percentage()).isNull());
        source.window=new CompletedAttemptReader.Window(List.of(completed(2,item(1,Topic.P01,WRONG)),completed(1,item(1,Topic.P01,CORRECT))),false);
        var result=use(source).overview(ACCOUNT,ROLE);
        assertThat(result.window().attemptCount()).isEqualTo(2);assertThat(result.window().latestFinishedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(result.window().oldestFinishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(metric(result.summary(),Topic.P01).assessed()).isEqualTo(1);
    }
    @Test void visitorDoesNotReadTheAccountWindow() {
        var source=new Source();
        assertThatThrownBy(() -> use(source).overview(new AttemptOwner(AttemptOwner.Kind.VISITOR,id(2)),ROLE)).isInstanceOf(Attempt.Unavailable.class);
        assertThat(source.calls).isZero();
    }
    @Test void individualVisitorOrAccountUsesOnlyTheRequestedCompletedAttempt() {
        for(var kind:AttemptOwner.Kind.values()) {
            var actor=new AttemptOwner(kind,id(3));var source=new Source();
            source.single=new CompletedAttempt(id(10),actor,ROLE,NOW,List.of(item(1,Topic.P01,BLANK)));
            source.window=new CompletedAttemptReader.Window(List.of(completed(1,item(2,Topic.P02,CORRECT))),false);
            var result=use(source).individual(actor,id(10));
            assertThat(metric(result.summary(),Topic.P01).blank()).isEqualTo(1);
            assertThat(metric(result.summary(),Topic.P02).assessed()).isZero();assertThat(source.calls).isEqualTo(1);
        }
    }
    @Test void wrongOwnerRoleIdOrderDuplicatesOrPartialCutNeverBecomePartialSuccess() {
        var a=completed(1,item(1,Topic.P01,CORRECT));var b=completed(2,item(2,Topic.P01,WRONG));var source=new Source();
        var otherOwner=new CompletedAttempt(id(3),new AttemptOwner(AttemptOwner.Kind.ACCOUNT,id(9)),ROLE,NOW,a.items());
        var otherRole=new CompletedAttempt(id(4),ACCOUNT,TargetRole.SCHOOL_SECRETARY,NOW,a.items());
        for(var list:List.of(List.of(a,b),List.of(a,a),List.of(otherOwner),List.of(otherRole))) {
            source.window=new CompletedAttemptReader.Window(list,false);
            assertThatThrownBy(() -> use(source).overview(ACCOUNT,ROLE)).isInstanceOf(IllegalStateException.class);
        }
        source.window=new CompletedAttemptReader.Window(List.of(a),true);
        assertThatThrownBy(() -> use(source).overview(ACCOUNT,ROLE)).isInstanceOf(IllegalStateException.class);
        source.single=otherOwner;assertThatThrownBy(() -> use(source).individual(ACCOUNT,id(3))).isInstanceOf(IllegalStateException.class);
        source.single=a;assertThatThrownBy(() -> use(source).individual(ACCOUNT,id(4))).isInstanceOf(IllegalStateException.class);
    }
    @Test void sourceFailureAndBusinessRejectionAreNotSwallowedAsEmptyData() {
        var source=new Source();
        for(var failure:List.of(new IllegalStateException("Falha sintética"),new Attempt.Unavailable(),new Attempt.Conflict())) {
            source.failure=failure;
            assertThatThrownBy(() -> use(source).overview(ACCOUNT,ROLE)).isSameAs(failure);
            assertThatThrownBy(() -> use(source).individual(ACCOUNT,id(1))).isSameAs(failure);
        }
    }
    @Test void fullWindowCarriesOlderFlagWithoutLeakingExtraEvidence() {
        var source=new Source();var list=new ArrayList<CompletedAttempt>();
        for(int i=100;i>0;i--)list.add(completed(i,item(i,Topic.P01,CORRECT)));
        source.window=new CompletedAttemptReader.Window(list,true);var r=use(source).overview(ACCOUNT,ROLE);
        assertThat(r.window().hasOlder()).isTrue();assertThat(r.window().attemptCount()).isEqualTo(100);
        assertThat(metric(r.summary(),Topic.P01).assessed()).isEqualTo(100);
    }
}

package br.com.concursosimulator.subjectreview;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.subjectreview.domain.ReviewSummary;
import org.junit.jupiter.api.Test;
import java.util.*;
import static br.com.concursosimulator.subjectreview.ReviewTestData.*;
import static br.com.concursosimulator.mockexam.domain.CompletedAttempt.Outcome.*;
import static org.assertj.core.api.Assertions.*;

class ReviewSummaryTests {
    @Test void emptyMeansNoDataForAllFiftyOneSubjects() {
        var summary=ReviewSummary.calculate(ROLE,List.of());
        assertThat(summary.subjects()).hasSize(51).allSatisfy(m -> {
            assertThat(m.assessed()).isZero(); assertThat(m.exposures()).isZero();
            assertThat(m.percentage()).isNull(); assertThat(m.lastFinishedAt()).isNull();
        });
        assertThat(summary.priorities()).isEmpty();
        assertThatThrownBy(() -> summary.subjects().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void latestBaseWinsBeforeGroupingWhileExposuresKeepCapturedTopics() {
        var old=completed(1,item(10,Topic.P01,CORRECT));
        var recent=completed(2,item(10,Topic.P02,WRONG));
        var s=ReviewSummary.calculate(ROLE,List.of(recent,old));
        assertThat(metric(s,Topic.P01).assessed()).isZero(); assertThat(metric(s,Topic.P01).exposures()).isEqualTo(1);
        assertThat(metric(s,Topic.P02).wrong()).isEqualTo(1); assertThat(metric(s,Topic.P02).lastFinishedAt()).isEqualTo(recent.finishedAt());
        assertThat(s.priorities()).isEmpty();
    }
    @Test void uuidTieBreakMatchesUnsignedPostgresOrdering() {
        var a=new CompletedAttempt(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"),ACCOUNT,ROLE,NOW,List.of(item(1,Topic.M04,CORRECT)));
        var b=new CompletedAttempt(UUID.fromString("80000000-0000-0000-0000-000000000000"),ACCOUNT,ROLE,NOW,List.of(item(1,Topic.M04,WRONG)));
        var m=metric(ReviewSummary.calculate(ROLE,List.of(b,a)),Topic.M04);
        assertThat(m.wrong()).isEqualTo(1); assertThat(m.exposures()).isEqualTo(2); assertThat(m.assessed()).isEqualTo(1);
    }
    @Test void blankRoundingSmallSampleAndAllCorrectHaveExplicitSemantics() {
        var s=ReviewSummary.calculate(ROLE,List.of(completed(1,item(1,Topic.P01,CORRECT),item(2,Topic.P01,CORRECT),item(3,Topic.P01,BLANK),
                item(4,Topic.P02,WRONG),item(5,Topic.P02,WRONG),item(6,Topic.P03,CORRECT),item(7,Topic.P03,CORRECT),item(8,Topic.P03,CORRECT))));
        assertThat(metric(s,Topic.P01).percentage()).isEqualByComparingTo("66.67");
        assertThat(metric(s,Topic.P01).blank()).isEqualTo(1); assertThat(s.priorities()).containsExactly(Topic.P01);
    }
    @Test void prioritiesUseExactFractionsBeforeRoundedPercentages() {
        var attempts=new ArrayList<CompletedAttempt>();
        for (var topic:List.of(Topic.P01,Topic.P02)) {
            int count=topic==Topic.P01?301:300;
            for (int offset=0;offset<count;offset+=100) {
                var items=new ArrayList<CompletedAttempt.Item>();
                for(int i=offset;i<Math.min(count,offset+100);i++) items.add(item((topic==Topic.P01?1000:2000)+i,topic,i==0?WRONG:CORRECT));
                attempts.add(new CompletedAttempt(id(attempts.size()+1),ACCOUNT,ROLE,NOW,items));
            }
        }
        var s=ReviewSummary.calculate(ROLE,attempts);
        assertThat(metric(s,Topic.P01).percentage()).isEqualByComparingTo(metric(s,Topic.P02).percentage());
        assertThat(s.priorities()).containsExactly(Topic.P02,Topic.P01);
    }
    @Test void priorityTiesUseMissedCountThenTopicId() {
        var s=ReviewSummary.calculate(ROLE,List.of(completed(1,
                item(1,Topic.P02,WRONG),item(2,Topic.P02,BLANK),item(3,Topic.P02,CORRECT),
                item(4,Topic.P01,WRONG),item(5,Topic.P01,BLANK),item(6,Topic.P01,CORRECT),
                item(7,Topic.P03,WRONG),item(8,Topic.P03,WRONG),item(9,Topic.P03,BLANK),item(10,Topic.P03,BLANK),item(11,Topic.P03,CORRECT),item(12,Topic.P03,CORRECT))));
        assertThat(s.priorities()).containsExactly(Topic.P03,Topic.P01,Topic.P02);
    }
    @Test void duplicateAttemptsOwnersRolesAndOversizedWindowFailInsteadOfProducingPartialData() {
        var a=completed(1,item(1,Topic.P01,CORRECT));
        assertThatThrownBy(() -> ReviewSummary.calculate(ROLE,List.of(a,a))).isInstanceOf(IllegalArgumentException.class);
        var other=new CompletedAttempt(id(2),new AttemptOwner(AttemptOwner.Kind.ACCOUNT,id(99)),ROLE,NOW,a.items());
        assertThatThrownBy(() -> ReviewSummary.calculate(ROLE,List.of(a,other))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewSummary.calculate(TargetRole.SCHOOL_SECRETARY,List.of(a))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewSummary.calculate(ROLE,Collections.nCopies(101,a))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompletedAttempt(id(3),ACCOUNT,ROLE,NOW,List.of(a.items().getFirst(),a.items().getFirst())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

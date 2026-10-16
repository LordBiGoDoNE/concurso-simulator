package br.com.concursosimulator.subjectreview;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

final class ReviewTestData {
    static final Instant NOW=Instant.parse("2026-10-16T10:00:00Z");
    static final AttemptOwner ACCOUNT=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,new UUID(0,1));
    static final TargetRole ROLE=TargetRole.ADMINISTRATIVE_ASSISTANT;
    static UUID id(long n) { return new UUID(0,n); }
    static CompletedAttempt.Item item(long base, Topic topic, CompletedAttempt.Outcome outcome) {
        return new CompletedAttempt.Item(id(base),new QuestionClassification(topic.discipline(),topic,Set.of(ROLE,TargetRole.SCHOOL_SECRETARY)),outcome);
    }
    static CompletedAttempt completed(long n, CompletedAttempt.Item... items) {
        return new CompletedAttempt(id(n),ACCOUNT,ROLE,NOW.plusSeconds(n),List.of(items));
    }
    static br.com.concursosimulator.subjectreview.domain.ReviewSummary.Metric metric(
            br.com.concursosimulator.subjectreview.domain.ReviewSummary s,Topic t) {
        return s.subjects().stream().filter(m -> m.topic()==t).findFirst().orElseThrow();
    }
}

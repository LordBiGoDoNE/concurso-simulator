package br.com.concursosimulator.subjectreview.web;

import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import br.com.concursosimulator.subjectreview.domain.ReviewSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ReviewContracts {
    private ReviewContracts() {}
    public record Subject(String topicId,String title,String disciplineId,String disciplineTitle,
                          int correct,int wrong,int blank,int assessed,int exposures,BigDecimal percentage,Instant lastFinishedAt) {
        static Subject from(ReviewSummary.Metric m) {
            var t=m.topic();return new Subject(t.id(),t.title(),t.discipline().id(),t.discipline().title(),
                    m.correct(),m.wrong(),m.blank(),m.assessed(),m.exposures(),m.percentage(),m.lastFinishedAt());
        }
    }
    public record Window(int limit,int attemptCount,boolean hasOlder,Instant latestFinishedAt,Instant oldestFinishedAt) {
        static Window from(ReviewUseCases.Window w) { return new Window(w.limit(),w.attemptCount(),w.hasOlder(),w.latestFinishedAt(),w.oldestFinishedAt()); }
    }
    public record Overview(String policy,String roleId,int minimumBases,Window window,List<Subject> subjects,List<String> priorityTopicIds) {
        static Overview from(ReviewUseCases.Overview o) { return new Overview(ReviewSummary.POLICY,o.role().id(),ReviewSummary.MIN_BASES,
                Window.from(o.window()),o.summary().subjects().stream().map(Subject::from).toList(),o.summary().priorities().stream().map(t->t.id()).toList()); }
    }
    public record Individual(String policy,String roleId,int minimumBases,UUID attemptId,List<Subject> subjects,List<String> priorityTopicIds) {
        static Individual from(ReviewUseCases.Individual i) { return new Individual(ReviewSummary.POLICY,i.role().id(),ReviewSummary.MIN_BASES,
                i.attemptId(),i.summary().subjects().stream().map(Subject::from).toList(),i.summary().priorities().stream().map(t->t.id()).toList()); }
    }
    public record Error(String error) {}
}

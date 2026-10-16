package br.com.concursosimulator.subjectreview.application;

import br.com.concursosimulator.mockexam.application.CompletedAttemptReader;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import br.com.concursosimulator.subjectreview.domain.ReviewSummary;
import java.time.*;
import java.util.*;

public final class ReviewUseCases {
    public record Window(int limit, int attemptCount, boolean hasOlder, Instant latestFinishedAt, Instant oldestFinishedAt) {}
    public record Overview(TargetRole role, Window window, ReviewSummary summary) {}
    public record Individual(UUID attemptId, TargetRole role, ReviewSummary summary) {}
    private final CompletedAttemptReader evidence;
    private final Clock clock;
    public ReviewUseCases(CompletedAttemptReader evidence, Clock clock) {
        this.evidence=Objects.requireNonNull(evidence);this.clock=Objects.requireNonNull(clock);
    }
    public Overview overview(AttemptOwner actor, TargetRole role) {
        Objects.requireNonNull(actor);Objects.requireNonNull(role);
        if (actor.kind()!=AttemptOwner.Kind.ACCOUNT) throw new Attempt.Unavailable();
        var window=evidence.recent(actor,role,ReviewSummary.WINDOW_LIMIT,clock.instant());
        require(window!=null);var attempts=window.attempts();
        require(!window.hasOlder() || attempts.size()==ReviewSummary.WINDOW_LIMIT);
        var ids=new HashSet<UUID>();CompletedAttempt previous=null;
        for(var a:attempts) {
            require(a.owner().equals(actor) && a.role()==role && ids.add(a.id()));
            require(previous==null || CompletedAttempt.ORDER.compare(previous,a)>0);previous=a;
        }
        return new Overview(role,new Window(ReviewSummary.WINDOW_LIMIT,attempts.size(),window.hasOlder(),
                attempts.isEmpty()?null:attempts.getFirst().finishedAt(),attempts.isEmpty()?null:attempts.getLast().finishedAt()),
                ReviewSummary.calculate(role,attempts));
    }
    public Individual individual(AttemptOwner actor, UUID id) {
        Objects.requireNonNull(actor);Objects.requireNonNull(id);
        var a=evidence.one(actor,id,clock.instant());
        require(a!=null && a.id().equals(id) && a.owner().equals(actor));
        return new Individual(a.id(),a.role(),ReviewSummary.calculate(a.role(),List.of(a)));
    }
    private static void require(boolean valid) {
        if (!valid) throw new IllegalStateException("Contrato de evidência concluída inconsistente");
    }
}

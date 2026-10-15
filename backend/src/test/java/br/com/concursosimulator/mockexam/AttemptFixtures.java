package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.QuestionFixtures;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

public final class AttemptFixtures {
    public static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    public static final AttemptOwner ACCOUNT = new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());
    public static final AttemptOwner VISITOR = new AttemptOwner(AttemptOwner.Kind.VISITOR,UUID.randomUUID());
    public static Composition composition(int count) {
        return new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,
                Map.of(Discipline.PORTUGUESE,new Composition.Portion(count,Set.of())));
    }
    public static AttemptQuestion question(int number) {
        var c = QuestionFixtures.content("Fixture de simulado " + number);
        return new AttemptQuestion(UUID.randomUUID(),UUID.randomUUID(),c.fingerprint(),c);
    }
    public static List<AttemptQuestion> pool(int count) {
        return java.util.stream.IntStream.range(0,count).mapToObj(AttemptFixtures::question).toList();
    }
    public static Attempt attempt(AttemptOwner owner, int count) {
        var items = pool(count).stream().map(q -> new Attempt.Item(UUID.randomUUID(),q,
                List.of(QuestionFixtures.SECOND,QuestionFixtures.FIRST),Map.of())).toList();
        return Attempt.start(UUID.randomUUID(),owner,composition(count),NOW,items);
    }
}

package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

/** Evidência mínima concluída; não contém enunciados, alternativas ou captura privada. */
public record CompletedAttempt(UUID id, AttemptOwner owner, TargetRole role, Instant finishedAt, List<Item> items) {
    public enum Outcome { CORRECT, WRONG, BLANK }
    public record Item(UUID questionId, QuestionClassification classification, Outcome outcome) {
        public Item {
            Objects.requireNonNull(questionId); Objects.requireNonNull(classification); Objects.requireNonNull(outcome);
        }
    }
    /** PostgreSQL compara UUID pelos bytes sem sinal; UUID.compareTo usa longs com sinal. */
    public static final Comparator<CompletedAttempt> ORDER = Comparator.comparing(CompletedAttempt::finishedAt)
            .thenComparing(CompletedAttempt::id, (a,b) -> {
                int high=Long.compareUnsigned(a.getMostSignificantBits(),b.getMostSignificantBits());
                return high!=0?high:Long.compareUnsigned(a.getLeastSignificantBits(),b.getLeastSignificantBits());
            });
    public CompletedAttempt {
        Objects.requireNonNull(id); Objects.requireNonNull(owner); Objects.requireNonNull(role); Objects.requireNonNull(finishedAt);
        items=List.copyOf(items);
        if (items.isEmpty() || items.size()>100) throw new IllegalArgumentException("Quantidade de evidências inválida");
        var bases=new HashSet<UUID>();
        for (var item:items) if (!bases.add(item.questionId()) || !item.classification().appliesTo(role))
            throw new IllegalArgumentException("Base repetida ou cargo divergente");
    }
}

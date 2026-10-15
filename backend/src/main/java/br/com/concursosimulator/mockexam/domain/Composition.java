package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;
import java.util.random.RandomGenerator;

public record Composition(TargetRole role, Map<Discipline, Portion> disciplines, boolean useParameterizedQuestions) {
    public Composition(TargetRole role, Map<Discipline, Portion> disciplines) { this(role,disciplines,false); }
    public record Portion(int quantity, Set<Topic> topics) {
        public Portion {
            if (quantity < 1 || quantity > 100) throw new IllegalArgumentException("Quantidade inválida");
            topics = Set.copyOf(topics);
        }
    }
    public Composition {
        Objects.requireNonNull(role); disciplines = Map.copyOf(disciplines);
        long total = disciplines.values().stream().mapToLong(Portion::quantity).sum();
        if (total < 1 || total > 100) throw new IllegalArgumentException("Total deve estar entre 1 e 100");
        disciplines.forEach((discipline, portion) -> {
            if (portion.topics().stream().anyMatch(t -> t.discipline() != discipline))
                throw new IllegalArgumentException("Assunto incompatível");
        });
    }
    public int total() { return disciplines.values().stream().mapToInt(Portion::quantity).sum(); }
    public boolean accepts(AttemptQuestion question) {
        var c = question.content().classification(); var p = disciplines.get(c.discipline());
        return p != null && c.roles().contains(role) && (p.topics().isEmpty() || p.topics().contains(c.topic()));
    }
    public List<AttemptQuestion> select(List<AttemptQuestion> pool, RandomGenerator random) {
        var ids = new HashSet<UUID>();
        for (var q : pool) if (!ids.add(q.questionId())) throw new IllegalStateException("Identidade duplicada no pool");
        var selected = new ArrayList<AttemptQuestion>(); var missing = new EnumMap<Discipline, Integer>(Discipline.class);
        for (var discipline : Discipline.values()) {
            var portion = disciplines.get(discipline); if (portion == null) continue;
            var candidates = pool.stream().filter(this::accepts)
                    .filter(q -> q.content().classification().discipline() == discipline).toList();
            if (candidates.size() < portion.quantity()) missing.put(discipline, candidates.size());
            else selected.addAll(shuffle(candidates, random).subList(0, portion.quantity()));
        }
        if (!missing.isEmpty()) throw new InsufficientPool(missing);
        return shuffle(selected, random);
    }
    public static <T> List<T> shuffle(List<T> input, RandomGenerator random) {
        var result = new ArrayList<>(input);
        for (int i = result.size() - 1; i > 0; i--) Collections.swap(result, i, random.nextInt(i + 1));
        return List.copyOf(result);
    }
    public static final class InsufficientPool extends RuntimeException {
        private final Map<Discipline, Integer> available;
        public InsufficientPool(Map<Discipline, Integer> available) {
            super("Acervo insuficiente para os critérios"); this.available = Map.copyOf(available);
        }
        public Map<Discipline, Integer> available() { return available; }
    }
}

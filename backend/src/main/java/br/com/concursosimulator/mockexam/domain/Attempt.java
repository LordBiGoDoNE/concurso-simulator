package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.*;
import java.util.*;

public final class Attempt {
    public static final String POLICY = "uniform-v1";
    public record Item(UUID id, AttemptQuestion question, List<UUID> optionOrder, Map<String,String> parameters) {
        public Item {
            Objects.requireNonNull(id); Objects.requireNonNull(question);
            optionOrder = List.copyOf(optionOrder); parameters = Map.copyOf(parameters);
            var ids = question.content().alternatives().stream().map(QuestionContent.Alternative::id).toList();
            var expected=question.source().capture()==null?Map.<String,String>of():question.source().capture().parameterStrings();
            if (!parameters.equals(expected) || optionOrder.size() != ids.size() || !new HashSet<>(optionOrder).equals(new HashSet<>(ids)))
                throw new IllegalArgumentException("Ordem/parâmetros inválidos");
        }
    }
    /** Somente persistência interna; contém material privado de correção. */
    public record Snapshot(UUID id, AttemptOwner owner, Composition composition, Instant createdAt,
                           Instant finishedAt, String policy, List<Item> items, Map<UUID,UUID> answers, long version, int format) {
        public Snapshot(UUID id, AttemptOwner owner, Composition composition, Instant createdAt, Instant finishedAt,
                        String policy, List<Item> items, Map<UUID,UUID> answers, long version) {
            this(id,owner,composition,createdAt,finishedAt,policy,items,answers,version,2);
        }
        public Snapshot { items = List.copyOf(items); answers = Map.copyOf(answers); }
    }
    public record Reference(String name, URI url) {}
    public record ItemView(UUID id, UUID questionId, UUID revisionId, QuestionClassification classification,
                           TextContent statement, List<QuestionContent.Alternative> alternatives,
                           List<Reference> references, UUID selectedAlternativeId) {}
    public record View(UUID id, TargetRole role, Instant createdAt, Instant finishedAt, long version, List<ItemView> items) {}
    public record Correction(ItemView item, UUID correctAlternativeId, TextContent resolution) {}
    public record Result(int total, int correct, int wrong, int blank, BigDecimal percentage, List<Correction> items) {}
    public static final class Unavailable extends RuntimeException {}
    public static final class Conflict extends RuntimeException {
        public Conflict() { super("Estado/versão da tentativa divergente"); }
    }
    private Snapshot state;
    private Attempt(Snapshot state) {
        Objects.requireNonNull(state.id()); Objects.requireNonNull(state.owner());
        Objects.requireNonNull(state.composition()); Objects.requireNonNull(state.createdAt());
        if ((state.format()!=1 && state.format()!=2) || state.format()==1 && state.composition().useParameterizedQuestions()
                || !POLICY.equals(state.policy()) || state.version() < 0 || state.items().size() != state.composition().total())
            throw new IllegalArgumentException("Snapshot de tentativa inválido");
        var itemIds = new HashSet<UUID>(); var questions = new HashSet<UUID>();
        var counts = new EnumMap<Discipline,Integer>(Discipline.class);
        for (var item : state.items()) {
            if (!state.composition().useParameterizedQuestions() && item.question().source().kind()!=QuestionSource.Kind.FIXED)
                throw new IllegalArgumentException("Geração não solicitada");
            if (!itemIds.add(item.id()) || !questions.add(item.question().questionId()) || !state.composition().accepts(item.question()))
                throw new IllegalArgumentException("Itens da composição inválidos");
            counts.merge(item.question().content().classification().discipline(), 1, Integer::sum);
        }
        state.composition().disciplines().forEach((d,p) -> {
            if (counts.getOrDefault(d,0) != p.quantity()) throw new IllegalArgumentException("Quantidade divergente");
        });
        this.state = state;
        state.answers().forEach(this::validateAnswer);
        if (state.finishedAt() != null && (state.finishedAt().isBefore(state.createdAt())
                || expiresAt() != null && !state.finishedAt().isBefore(expiresAt())))
            throw new IllegalArgumentException("Data de conclusão inválida");
    }
    public static Attempt start(UUID id, AttemptOwner owner, Composition composition, Instant at, List<Item> items) {
        return new Attempt(new Snapshot(id,owner,composition,at,null,POLICY,items,Map.of(),0));
    }
    public static Attempt restore(Snapshot snapshot) { return new Attempt(snapshot); }
    public Snapshot snapshot() { return state; }
    public Instant expiresAt() {
        return state.owner().kind() == AttemptOwner.Kind.VISITOR ? state.createdAt().plus(Duration.ofHours(24)) : null;
    }
    public void requireAccessible(AttemptOwner actor, Instant now) {
        if (!state.owner().equals(actor) || now.isBefore(state.createdAt()) || expiresAt() != null && !now.isBefore(expiresAt()))
            throw new Unavailable();
    }
    public void answer(AttemptOwner actor, Instant now, long expectedVersion, UUID itemId, UUID alternativeId) {
        editable(actor,now,expectedVersion); validateAnswer(itemId,alternativeId);
        var answers = new HashMap<>(state.answers());
        if (alternativeId == null) answers.remove(itemId); else answers.put(itemId,alternativeId);
        state = new Snapshot(state.id(),state.owner(),state.composition(),state.createdAt(),null,POLICY,
                state.items(),answers,Math.addExact(state.version(),1),state.format());
    }
    public void finish(AttemptOwner actor, Instant now, long expectedVersion) {
        editable(actor,now,expectedVersion);
        state = new Snapshot(state.id(),state.owner(),state.composition(),state.createdAt(),now,POLICY,
                state.items(),state.answers(),Math.addExact(state.version(),1),state.format());
    }
    private void editable(AttemptOwner actor, Instant now, long version) {
        requireAccessible(actor,now);
        if (state.finishedAt() != null || state.version() != version) throw new Conflict();
    }
    private void validateAnswer(UUID itemId, UUID alternativeId) {
        var item = state.items().stream().filter(i -> i.id().equals(itemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não pertence à tentativa"));
        if (alternativeId != null && !item.optionOrder().contains(alternativeId))
            throw new IllegalArgumentException("Alternativa não pertence ao item");
    }
    public View view(AttemptOwner actor, Instant now) {
        requireAccessible(actor,now);
        return new View(state.id(),state.composition().role(),state.createdAt(),state.finishedAt(),state.version(),
                state.items().stream().map(this::viewItem).toList());
    }
    private ItemView viewItem(Item item) {
        var q = item.question(); var c = q.content();
        var options = item.optionOrder().stream().map(id -> c.alternatives().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow()).toList();
        return new ItemView(item.id(),q.questionId(),q.revisionId(),c.classification(),c.statement(),options,
                c.references().stream().map(r -> new Reference(r.name(),r.url())).toList(),state.answers().get(item.id()));
    }
    public Result result(AttemptOwner actor, Instant now) {
        requireAccessible(actor,now); if (state.finishedAt() == null) throw new Conflict();
        int correct = 0, blank = 0;
        for (var item : state.items()) {
            switch (outcome(item)) { case CORRECT -> correct++; case BLANK -> blank++; case WRONG -> {} }
        }
        int total = state.items().size();
        return new Result(total,correct,total-correct-blank,blank,
                BigDecimal.valueOf(correct * 100L).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP),
                state.items().stream().map(i -> new Correction(viewItem(i),i.question().content().correctAlternativeId(),i.question().content().resolution())).toList());
    }
    public CompletedAttempt evidence(AttemptOwner actor, Instant now) {
        requireAccessible(actor,now); if (state.finishedAt()==null) throw new Conflict();
        return new CompletedAttempt(state.id(),state.owner(),state.composition().role(),state.finishedAt(),
                state.items().stream().map(i -> new CompletedAttempt.Item(i.question().questionId(),
                        i.question().content().classification(),outcome(i))).toList());
    }
    private CompletedAttempt.Outcome outcome(Item item) {
        var answer=state.answers().get(item.id());
        return answer==null?CompletedAttempt.Outcome.BLANK:answer.equals(item.question().content().correctAlternativeId())
                ?CompletedAttempt.Outcome.CORRECT:CompletedAttempt.Outcome.WRONG;
    }
}

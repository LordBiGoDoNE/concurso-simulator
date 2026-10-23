package br.com.concursosimulator.mockexam.application;

import br.com.concursosimulator.mockexam.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.random.RandomGenerator;

public final class AttemptUseCases {
    public record Outcome(Attempt.View attempt, boolean replayed) {}
    public record Finished(Attempt.View attempt, Attempt.Result result, boolean replayed) {}
    public record HistoryEntry(UUID id, String role, Instant createdAt, Instant finishedAt, int total,
                               java.math.BigDecimal percentage) {}
    private final AttemptStore store;
    private final Clock clock;
    private final Supplier<UUID> ids;
    private final Supplier<RandomGenerator> random;
    private final boolean generationEnabled;
    public AttemptUseCases(AttemptStore store, Clock clock, Supplier<UUID> ids, Supplier<RandomGenerator> random) {
        this(store,clock,ids,random,true);
    }
    public AttemptUseCases(AttemptStore store, Clock clock, Supplier<UUID> ids, Supplier<RandomGenerator> random, boolean generationEnabled) {
        this.store=Objects.requireNonNull(store); this.clock=Objects.requireNonNull(clock);
        this.ids=Objects.requireNonNull(ids); this.random=Objects.requireNonNull(random);
        this.generationEnabled=generationEnabled;
    }
    public Outcome create(AttemptOwner actor, UUID operation, Composition composition) {
        Objects.requireNonNull(actor); Objects.requireNonNull(operation); Objects.requireNonNull(composition);
        var hash=hash("create", canonical(composition));
        return execute(actor,operation,hash,null, session -> {
            if (composition.useParameterizedQuestions() && !generationEnabled)
                throw new IllegalStateException("Geração temporariamente desabilitada");
            var rng=random.get(); var selected=composition.select(session.eligible(),rng);
            var captured=composition.useParameterizedQuestions()
                    ?AttemptMaterializer.materialize(selected,session.templates(),rng,ids):selected;
            var items=captured.stream().map(q -> {
                var order=new ArrayList<>(q.content().alternatives().stream().map(a -> a.id()).toList());
                for (int i=order.size()-1;i>0;i--) Collections.swap(order,i,rng.nextInt(i+1));
                return new Attempt.Item(ids.get(),q,order,q.source().capture()==null?Map.of():q.source().capture().parameterStrings());
            }).toList();
            var attempt=Attempt.start(ids.get(),actor,composition,now(),items);
            session.insert(attempt.snapshot()); return attempt;
        });
    }
    public Outcome answer(AttemptOwner actor, UUID attemptId, UUID operation, long version, UUID item, UUID alternative) {
        Objects.requireNonNull(item);
        return execute(actor,operation,hash("answer",attemptId,version,item,alternative),attemptId, session -> {
            var attempt=load(session,actor,attemptId); attempt.answer(actor,now(),version,item,alternative);
            session.update(attempt.snapshot(),version); return attempt;
        });
    }
    public Finished finish(AttemptOwner actor, UUID attemptId, UUID operation, long version) {
        var outcome=execute(actor,operation,hash("finish",attemptId,version),attemptId, session -> {
            var attempt=load(session,actor,attemptId); attempt.finish(actor,now(),version);
            session.update(attempt.snapshot(),version); return attempt;
        });
        var result=store.transaction(s -> load(s,actor,attemptId).result(actor,now()));
        return new Finished(outcome.attempt(),result,outcome.replayed());
    }
    public Attempt.View read(AttemptOwner actor, UUID id) {
        return store.transaction(s -> load(s,actor,id).view(actor,now()));
    }
    public Attempt.Result result(AttemptOwner actor, UUID id) {
        return store.transaction(s -> load(s,actor,id).result(actor,now()));
    }
    public List<HistoryEntry> history(AttemptOwner actor, AttemptStore.HistoryQuery query) {
        Objects.requireNonNull(actor); Objects.requireNonNull(query);
        if (actor.kind()!=AttemptOwner.Kind.ACCOUNT) throw new Attempt.Unavailable();
        return store.transaction(s -> s.history(actor,query).stream().map(snapshot -> {
            var a=Attempt.restore(snapshot); a.requireAccessible(actor,now());
            return new HistoryEntry(snapshot.id(),snapshot.composition().role().name(),snapshot.createdAt(),snapshot.finishedAt(),
                    snapshot.items().size(),snapshot.finishedAt()==null?null:a.result(actor,now()).percentage());
        }).toList());
    }
    private Instant now() { return clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
    private Attempt load(AttemptStore.Session session, AttemptOwner actor, UUID id) {
        var attempt=Attempt.restore(session.find(Objects.requireNonNull(id)).orElseThrow(Attempt.Unavailable::new));
        attempt.requireAccessible(Objects.requireNonNull(actor),now()); return attempt;
    }
    private Outcome execute(AttemptOwner actor, UUID operation, String hash, UUID id, Function<AttemptStore.Session,Attempt> action) {
        Objects.requireNonNull(actor); Objects.requireNonNull(operation);
        try { return store.transaction(s -> command(s,actor,operation,hash,id,action)); }
        catch (AttemptStore.ConcurrentWrite conflict) {
            // A transação perdedora já foi revertida. Só um comando confirmado permite sucesso.
            return store.transaction(s -> {
                if (id!=null) load(s,actor,id);
                var prior=s.command(actor,operation).orElseThrow(Attempt.Conflict::new);
                return replay(prior,actor,hash);
            });
        }
    }
    private Outcome command(AttemptStore.Session session, AttemptOwner actor, UUID operation, String hash, UUID id,
                            Function<AttemptStore.Session,Attempt> action) {
        if (id!=null) load(session,actor,id); // Posse/expiração antecedem até o replay.
        var prior=session.command(actor,operation);
        if (prior.isPresent()) return replay(prior.get(),actor,hash);
        var attempt=action.apply(session);
        session.record(actor,operation,new AttemptStore.Command(hash,attempt.snapshot()));
        return new Outcome(attempt.view(actor,now()),false);
    }
    private Outcome replay(AttemptStore.Command prior, AttemptOwner actor, String hash) {
        var attempt=Attempt.restore(prior.outcome()); attempt.requireAccessible(actor,now());
        if (!prior.hash().equals(hash)) throw new Attempt.Conflict();
        return new Outcome(attempt.view(actor,now()),true);
    }
    private static String canonical(Composition c) {
        var parts=new ArrayList<String>(); parts.add(c.role().name());
        c.disciplines().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            parts.add(e.getKey().name()); parts.add(Integer.toString(e.getValue().quantity()));
            parts.add(e.getValue().topics().stream().map(Enum::name).sorted().toList().toString());
        });
        // Ausente/false deve produzir exatamente os bytes da V5, inclusive pontuação.
        return c.useParameterizedQuestions()?"parameterized-create/v1:"+parts:parts.toString();
    }
    private static String hash(Object... values) {
        try {
            var bytes=new ByteArrayOutputStream(); var data=new DataOutputStream(bytes);
            for (var value:values) { var b=String.valueOf(value).getBytes(StandardCharsets.UTF_8); data.writeInt(b.length); data.write(b); }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}

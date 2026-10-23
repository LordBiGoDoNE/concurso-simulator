package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;

/** Unidade de aplicação de candidatos já estruturalmente válidos; não extrai nem aprova conteúdo. */
public final class ApplyQuestionCandidatesUseCase {
    private final QuestionStore store;
    public ApplyQuestionCandidatesUseCase(QuestionStore store) { this.store=Objects.requireNonNull(store); }
    public record Candidate(QuestionOrigin origin,QuestionContent content,UUID expectedLatestRevision) {
        public Candidate { Objects.requireNonNull(origin); Objects.requireNonNull(content); }
    }
    public record Result(QuestionOrigin origin,UUID questionId,UUID revisionId,boolean reused) {}
    public List<Result> execute(List<Candidate> candidates) {
        var selected=List.copyOf(candidates); var origins=new HashSet<QuestionOrigin>();
        for (var item:selected) if (!origins.add(item.origin())) throw new IllegalArgumentException("Origem repetida no pacote");
        // Ordem fixa de aquisição de travas evita deadlock entre pacotes com as mesmas origens.
        var ordered=selected.stream().sorted(Comparator.comparing((Candidate c)->c.origin().namespace()).thenComparing(c->c.origin().key())).toList();
        try { return apply(ordered); }
        catch(OriginAlreadyExistsException conflict) {
            // O contrato garante que a unidade anterior terminou em rollback. Nunca continuar nela.
            return apply(ordered); // Uma nova unidade; segunda colisão/falha é propagada.
        }
    }
    private List<Result> apply(List<Candidate> candidates) {
        return store.transaction(session->{
            var results=new ArrayList<Result>();
            for(var candidate:candidates) {
                var existing=session.findOriginForUpdate(candidate.origin());
                if(existing.isEmpty()) {
                    if(candidate.expectedLatestRevision()!=null) throw new StaleQuestionException();
                    var q=Question.draft(UUID.randomUUID(),candidate.origin(),UUID.randomUUID(),candidate.content());
                    session.insert(q);results.add(new Result(q.origin(),q.id(),q.latest().id(),false));continue;
                }
                var q=existing.get();var hash=candidate.content().fingerprint();
                var same=q.revisions().stream().filter(r->r.fingerprint().equals(hash)).findFirst();
                if(same.isPresent()) {
                    if(!same.get().content().equals(candidate.content())) throw new IllegalStateException("Fingerprint colidente");
                    results.add(new Result(q.origin(),q.id(),same.get().id(),true));continue; // Não mudar estado/ponteiro/histórico.
                }
                if(!q.latest().id().equals(candidate.expectedLatestRevision())) throw new StaleQuestionException();
                var next=q.append(UUID.randomUUID(),candidate.content());session.save(q);
                results.add(new Result(q.origin(),q.id(),next.id(),false));
            }
            return List.copyOf(results);
        });
    }
}

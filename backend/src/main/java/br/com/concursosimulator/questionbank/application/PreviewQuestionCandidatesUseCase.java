package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.QuestionInventory;
import br.com.concursosimulator.questionbank.domain.QuestionOrigin;
import java.util.*;

/** Prévia informativa; o comando de escrita revalida a base sob trava na sua unidade atômica. */
public final class PreviewQuestionCandidatesUseCase {
    private final QuestionInventory inventory;
    public PreviewQuestionCandidatesUseCase(QuestionInventory inventory) { this.inventory = Objects.requireNonNull(inventory); }
    public enum Change { NEW, IDENTICAL, CHANGED, STALE }
    public record Result(QuestionOrigin origin, Change change, UUID questionId, UUID latestRevisionId) {}
    public List<Result> execute(List<ApplyQuestionCandidatesUseCase.Candidate> candidates) {
        var existing = new HashMap<QuestionOrigin, QuestionInventory.Entry>();
        for (var entry : inventory.read()) existing.put(entry.origin(), entry);
        var seen = new HashSet<QuestionOrigin>(); var results = new ArrayList<Result>();
        for (var candidate : List.copyOf(candidates)) {
            if (!seen.add(candidate.origin())) throw new IllegalArgumentException("Origem repetida na seleção");
            var entry = existing.get(candidate.origin());
            var change = entry == null ? (candidate.expectedLatestRevision() == null ? Change.NEW : Change.STALE)
                    : entry.revisions().stream().anyMatch(r -> r.fingerprint().equals(candidate.content().fingerprint())) ? Change.IDENTICAL
                    : entry.latest().id().equals(candidate.expectedLatestRevision()) ? Change.CHANGED : Change.STALE;
            results.add(new Result(candidate.origin(), change, entry == null ? null : entry.questionId(), entry == null ? null : entry.latest().id()));
        }
        return List.copyOf(results);
    }
}

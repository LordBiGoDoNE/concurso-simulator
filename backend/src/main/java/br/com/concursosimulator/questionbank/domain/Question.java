package br.com.concursosimulator.questionbank.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Identidade estável; alterar conteúdo acrescenta revisão, nunca reescreve a anterior. */
public final class Question {
    private final UUID id;
    private final QuestionOrigin origin;
    private final List<QuestionRevision> revisions;

    public Question(UUID id, QuestionOrigin origin, List<QuestionRevision> revisions) {
        this.id = Objects.requireNonNull(id); this.origin = Objects.requireNonNull(origin);
        this.revisions = new ArrayList<>(List.copyOf(revisions));
        var ids = new HashSet<UUID>();
        if (revisions.isEmpty()) throw new IllegalArgumentException("Questão exige uma revisão");
        for (int i = 0; i < revisions.size(); i++) {
            if (revisions.get(i).number() != i + 1 || !ids.add(revisions.get(i).id())) {
                throw new IllegalArgumentException("Revisões devem ser únicas e sequenciais");
            }
        }
    }
    public static Question draft(UUID id, QuestionOrigin origin, UUID revisionId, QuestionContent content) {
        return new Question(id, origin, List.of(new QuestionRevision(revisionId, 1, content)));
    }
    public QuestionRevision append(UUID revisionId, QuestionContent content) {
        if (revisions.stream().anyMatch(r -> r.id().equals(revisionId))) throw new IllegalArgumentException("ID de revisão repetido");
        var revision = new QuestionRevision(revisionId, revisions.size() + 1, content);
        revisions.add(revision); return revision;
    }
    public UUID id() { return id; }
    public QuestionOrigin origin() { return origin; }
    public List<QuestionRevision> revisions() { return List.copyOf(revisions); }
    public QuestionRevision latest() { return revisions.getLast(); }
    public QuestionRevision revision(UUID revisionId) {
        return revisions.stream().filter(r -> r.id().equals(revisionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Revisão não pertence à questão"));
    }
}

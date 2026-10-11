package br.com.concursosimulator.questionbank.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Identidade estável; alterar conteúdo acrescenta revisão, nunca reescreve a anterior. */
public final class Question {
    private final UUID id;
    private final QuestionOrigin origin;
    private final List<QuestionRevision> revisions;
    public enum Status { DRAFT, IN_REVIEW, APPROVED }
    private final Map<UUID, Status> statuses = new HashMap<>();
    private final List<EditorialDecision> decisions = new ArrayList<>();
    private UUID eligibleRevisionId;

    public Question(UUID id, QuestionOrigin origin, List<QuestionRevision> revisions) {
        this.id = Objects.requireNonNull(id); this.origin = Objects.requireNonNull(origin);
        this.revisions = new ArrayList<>(List.copyOf(revisions));
        var ids = new HashSet<UUID>();
        if (revisions.isEmpty()) throw new IllegalArgumentException("Questão exige uma revisão");
        for (int i = 0; i < revisions.size(); i++) {
            if (revisions.get(i).number() != i + 1 || !ids.add(revisions.get(i).id())) {
                throw new IllegalArgumentException("Revisões devem ser únicas e sequenciais");
            }
            statuses.put(revisions.get(i).id(), Status.DRAFT);
        }
    }
    public static Question draft(UUID id, QuestionOrigin origin, UUID revisionId, QuestionContent content) {
        return new Question(id, origin, List.of(new QuestionRevision(revisionId, 1, content)));
    }
    public QuestionRevision append(UUID revisionId, QuestionContent content) {
        if (revisions.stream().anyMatch(r -> r.id().equals(revisionId))) throw new IllegalArgumentException("ID de revisão repetido");
        var revision = new QuestionRevision(revisionId, revisions.size() + 1, content);
        revisions.add(revision); statuses.put(revisionId, Status.DRAFT); return revision;
    }
    public UUID id() { return id; }
    public QuestionOrigin origin() { return origin; }
    public List<QuestionRevision> revisions() { return List.copyOf(revisions); }
    public QuestionRevision latest() { return revisions.getLast(); }
    public QuestionRevision revision(UUID revisionId) {
        return revisions.stream().filter(r -> r.id().equals(revisionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Revisão não pertence à questão"));
    }
    public static Question restore(UUID id, QuestionOrigin origin, List<QuestionRevision> revisions, List<EditorialDecision> history) {
        var question = new Question(id, origin, revisions);
        for (var decision : List.copyOf(history)) question.decide(decision);
        return question;
    }
    public Status status(UUID revisionId) { revision(revisionId); return statuses.get(revisionId); }
    public Optional<QuestionRevision> eligible() { return Optional.ofNullable(eligibleRevisionId).map(this::revision); }
    public List<EditorialDecision> decisions() { return List.copyOf(decisions); }
    public void decide(EditorialDecision decision) {
        var revision = revision(decision.revisionId());
        if (!id.equals(decision.questionId()) || !revision.fingerprint().equals(decision.fingerprint())) {
            throw new IllegalArgumentException("Evidência não corresponde à questão/revisão/conteúdo");
        }
        var current = status(revision.id());
        switch (decision.action()) {
            case SUBMIT -> {
                requireStatus(current, Status.DRAFT); statuses.put(revision.id(), Status.IN_REVIEW);
            }
            case RETURN_TO_DRAFT -> {
                requireStatus(current, Status.IN_REVIEW); statuses.put(revision.id(), Status.DRAFT);
            }
            case APPROVE -> {
                requireStatus(current, Status.IN_REVIEW);
                if (!decision.checklist().containsAll(Set.of(EditorialDecision.Check.values()))) {
                    throw new IllegalArgumentException("Checklist editorial incompleto");
                }
                if (revisions.stream().anyMatch(r -> r.number() > revision.number() && status(r.id()) == Status.APPROVED)) {
                    throw new IllegalStateException("Não regredir revisão já aprovada");
                }
                statuses.put(revision.id(), Status.APPROVED); eligibleRevisionId = revision.id();
            }
            case WITHDRAW -> {
                if (!revision.id().equals(eligibleRevisionId)) throw new IllegalStateException("Revisão não está elegível");
                eligibleRevisionId = null; // Conteúdo/status aprovado/histórico permanecem íntegros.
            }
        }
        decisions.add(decision);
    }
    private static void requireStatus(Status actual, Status expected) {
        if (actual != expected) throw new IllegalStateException("Transição editorial inválida: " + actual + " (esperado " + expected + ")");
    }
}

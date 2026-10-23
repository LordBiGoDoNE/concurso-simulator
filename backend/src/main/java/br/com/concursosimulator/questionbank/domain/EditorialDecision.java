package br.com.concursosimulator.questionbank.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Evidência editorial da revisão exata; não é credencial/autorização Google. */
public record EditorialDecision(Action action, UUID questionId, UUID revisionId, String fingerprint,
                               String responsible, Instant at, Set<Check> checklist, String rationale) {
    public enum Action { SUBMIT, RETURN_TO_DRAFT, APPROVE, WITHDRAW }
    public enum Check { CORRECTNESS, DISTRACTORS, CLARITY, STEP_BY_STEP, ACCESSIBILITY, SOURCES, CLASSIFICATION }
    public EditorialDecision {
        Objects.requireNonNull(action); Objects.requireNonNull(questionId); Objects.requireNonNull(revisionId);
        ContentRules.hash(fingerprint); ContentRules.text(responsible, 500, "Responsável editorial");
        Objects.requireNonNull(at); checklist = Set.copyOf(checklist);
        ContentRules.text(rationale, 4000, "Justificativa/link de revisão");
    }
}

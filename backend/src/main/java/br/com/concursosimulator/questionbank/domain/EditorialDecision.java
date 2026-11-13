package br.com.concursosimulator.questionbank.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Evidência editorial da revisão exata; não é credencial/autorização Google.
 * {@code reviewKind} registra quem avaliou a revisão (par humano ou somente IA); nulo só em
 * histórico anterior ao campo ou em ações diferentes de APPROVE, nunca inferido.
 */
public record EditorialDecision(Action action, UUID questionId, UUID revisionId, String fingerprint,
                               String responsible, Instant at, Set<Check> checklist, String rationale,
                               ReviewKind reviewKind) {
    public enum Action { SUBMIT, RETURN_TO_DRAFT, APPROVE, WITHDRAW }
    public enum Check { CORRECTNESS, DISTRACTORS, CLARITY, STEP_BY_STEP, ACCESSIBILITY, SOURCES, CLASSIFICATION }
    /** Origem da avaliação, não do texto: a mesma revisão pode ser reavaliada. */
    public enum ReviewKind { HUMAN_PEER, AI_ONLY }
    public EditorialDecision {
        Objects.requireNonNull(action); Objects.requireNonNull(questionId); Objects.requireNonNull(revisionId);
        ContentRules.hash(fingerprint); ContentRules.text(responsible, 500, "Responsável editorial");
        Objects.requireNonNull(at); checklist = Set.copyOf(checklist);
        ContentRules.text(rationale, 4000, "Justificativa/link de revisão");
    }
}

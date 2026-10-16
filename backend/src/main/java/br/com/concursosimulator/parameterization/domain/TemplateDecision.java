package br.com.concursosimulator.parameterization.domain;

import java.time.Instant;
import java.util.*;

/** Evidência editorial do conjunto exato, não autenticação nem aprovação produzida por testes. */
public record TemplateDecision(UUID operationId,UUID templateId,int version,Action action,String definitionDigest,
                               String domainDigest,String manifestDigest,UUID baseRevisionId,String baseFingerprint,
                               String responsible,Instant at,Set<Check> checklist,String rationale) {
    public enum Action { SUBMIT, RETURN_TO_DRAFT, APPROVE, WITHDRAW, ACTIVATE }
    public enum Check { CORRECTNESS, DISTRACTORS, CLARITY, STEP_BY_STEP, ACCESSIBILITY, SOURCES, CLASSIFICATION, COMPLETE_DOMAIN }
    public TemplateDecision {
        Objects.requireNonNull(operationId);Objects.requireNonNull(templateId);Objects.requireNonNull(action);
        Objects.requireNonNull(baseRevisionId);Objects.requireNonNull(at);checklist=Set.copyOf(checklist);
        CanonicalDigest.hash(definitionDigest);CanonicalDigest.hash(domainDigest);CanonicalDigest.hash(manifestDigest);CanonicalDigest.hash(baseFingerprint);
        if(version<1)throw new IllegalArgumentException("Versão inválida");
        new br.com.concursosimulator.questionbank.domain.TextContent(responsible,List.of());
        new br.com.concursosimulator.questionbank.domain.TextContent(rationale,List.of());
        if(responsible.length()>500 || rationale.length()>4000)throw new IllegalArgumentException("Evidência editorial acima do limite");
    }
}

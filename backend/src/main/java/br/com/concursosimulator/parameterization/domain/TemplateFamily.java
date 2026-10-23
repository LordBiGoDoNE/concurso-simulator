package br.com.concursosimulator.parameterization.domain;

import java.util.*;

/** Agregado: histórico aprovado e ponteiro elegível são estados diferentes. */
public final class TemplateFamily {
    public enum Status { DRAFT, IN_REVIEW, APPROVED }
    private final List<TemplateVersion> versions=new ArrayList<>();
    private final List<TemplateDecision> decisions=new ArrayList<>();
    private final Map<Integer,Status> statuses=new HashMap<>();
    private Integer eligibleVersion;
    public TemplateFamily(TemplateVersion first) { append(first); }
    public static TemplateFamily restore(List<TemplateVersion> versions,List<TemplateDecision> history) {
        if(versions.isEmpty())throw new IllegalArgumentException("Família vazia");
        if(history.stream().map(TemplateDecision::operationId).distinct().count()!=history.size())throw new IllegalArgumentException("Histórico editorial duplicado");
        var family=new TemplateFamily(versions.getFirst());versions.stream().skip(1).forEach(family::append);
        history.forEach(family::decide);return family;
    }
    public void append(TemplateVersion v) {
        var d=v.definition();
        if(d.version()!=versions.size()+1 || !versions.isEmpty() &&
                (!id().equals(d.id()) || !baseQuestionId().equals(d.baseQuestionId()) || versions.getFirst().definition().family()!=d.family()))
            throw new IllegalArgumentException("Versão não sequencial ou identidade divergente");
        versions.add(v);statuses.put(d.version(),Status.DRAFT);
    }
    public UUID id() {return versions.getFirst().definition().id();}
    public UUID baseQuestionId() {return versions.getFirst().definition().baseQuestionId();}
    public List<TemplateVersion> versions() {return List.copyOf(versions);}
    public List<TemplateDecision> decisions() {return List.copyOf(decisions);}
    public int sequence() {return decisions.size();}
    public Integer eligibleVersion() {return eligibleVersion;}
    public Optional<TemplateVersion> eligible() {return Optional.ofNullable(eligibleVersion).map(this::version);}
    public TemplateVersion version(int number) {
        if(number<1 || number>versions.size())throw new IllegalArgumentException("Versão inexistente");return versions.get(number-1);
    }
    public Status status(int number) {version(number);return statuses.get(number);}
    public boolean replayed(TemplateDecision d) {
        var prior=decisions.stream().filter(p -> p.operationId().equals(d.operationId())).findFirst();
        if(prior.isPresent() && !prior.get().equals(d))throw new IllegalStateException("Operação editorial reutilizada com outro conteúdo");
        return prior.isPresent();
    }
    public void decide(TemplateDecision decision) {
        if(replayed(decision))return;
        var v=version(decision.version());var d=v.definition();
        if(!id().equals(decision.templateId()) || !d.digest().equals(decision.definitionDigest())
                || !d.family().domainDigest().equals(decision.domainDigest()) || !v.manifest().digest().equals(decision.manifestDigest())
                || !d.baseRevisionId().equals(decision.baseRevisionId()) || !d.baseFingerprint().equals(decision.baseFingerprint()))
            throw new IllegalArgumentException("Evidência não corresponde à versão/conjunto exatos");
        var current=status(d.version());
        switch(decision.action()) {
            case SUBMIT -> {require(current,Status.DRAFT);statuses.put(d.version(),Status.IN_REVIEW);}
            case RETURN_TO_DRAFT -> {require(current,Status.IN_REVIEW);statuses.put(d.version(),Status.DRAFT);}
            case APPROVE -> {
                require(current,Status.IN_REVIEW);requireChecklist(decision);
                if(statuses.entrySet().stream().anyMatch(e -> e.getKey()>d.version() && e.getValue()==Status.APPROVED))
                    throw new IllegalStateException("Não regredir aprovação de versão");
                statuses.put(d.version(),Status.APPROVED);eligibleVersion=d.version();
            }
            case ACTIVATE -> {require(current,Status.APPROVED);requireChecklist(decision);eligibleVersion=d.version();}
            case WITHDRAW -> {require(current,Status.APPROVED);if(Objects.equals(eligibleVersion,d.version()))eligibleVersion=null;}
        }
        decisions.add(decision);
    }
    private static void require(Status actual,Status expected) {
        if(actual!=expected)throw new IllegalStateException("Transição editorial inválida");
    }
    private static void requireChecklist(TemplateDecision d) {
        if(!d.checklist().containsAll(Set.of(TemplateDecision.Check.values())))throw new IllegalArgumentException("Checklist editorial incompleto");
    }
}

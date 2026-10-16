package br.com.concursosimulator.parameterization.application;

import br.com.concursosimulator.parameterization.domain.*;
import java.util.*;

public final class TemplateUseCases {
    public record Registered(TemplateVersion version,boolean replayed) {}
    private final TemplateStore store;
    public TemplateUseCases(TemplateStore store){this.store=Objects.requireNonNull(store);}
    /** Sem I/O/DB: a prévia não exige conexão nem grava decisões. */
    public static TemplateVersion preview(TemplateDefinition definition){return new TemplateVersion(definition,VariantManifest.create(definition));}
    public Registered register(TemplateDefinition definition,int expectedLatest) {
        var v=preview(definition);if(expectedLatest<0)throw new IllegalArgumentException("Base esperada inválida");
        try {return store.transaction(s -> register(s,v,expectedLatest));}
        catch(TemplateStore.ConcurrentRegistration conflict) {
            // Adaptador já concluiu rollback. Só conteúdo idêntico confirmado permite sucesso.
            return store.transaction(s -> {
                var t=s.findBaseForUpdate(definition.baseQuestionId()).orElseThrow(TemplateStore.Stale::new);
                if(!t.id().equals(definition.id()) || t.versions().size()<definition.version() || !t.version(definition.version()).equals(v))throw new TemplateStore.Stale();
                return new Registered(t.version(definition.version()),true);
            });
        }
    }
    private Registered register(TemplateStore.Session s,TemplateVersion v,int expectedLatest) {
        var d=v.definition();var existing=s.findBaseForUpdate(d.baseQuestionId());
        if(existing.isPresent()) {
            var t=existing.get();
            if(!t.id().equals(d.id()))throw new IllegalArgumentException("Base já pertence a outra família");
            if(d.version()<=t.versions().size()) {
                if(!t.version(d.version()).equals(v))throw new TemplateStore.Stale();
                return new Registered(t.version(d.version()),true);
            }
            if(t.versions().size()!=expectedLatest)throw new TemplateStore.Stale();
            requireBase(s,d);t.append(v);s.save(t);
        } else {
            if(expectedLatest!=0)throw new TemplateStore.Stale();requireBase(s,d);s.insert(new TemplateFamily(v));
        }
        return new Registered(v,false);
    }
    public boolean decide(TemplateDecision decision,Integer expectedEligible,int expectedSequence) {
        Objects.requireNonNull(decision);
        if(expectedSequence<0 || expectedEligible!=null && expectedEligible<1)throw new IllegalArgumentException("Estado esperado inválido");
        return store.transaction(s -> {
            var t=s.loadForUpdate(decision.templateId()).orElseThrow(() -> new IllegalArgumentException("Família inexistente"));
            if(t.replayed(decision))return true;
            if(t.sequence()!=expectedSequence || !Objects.equals(t.eligibleVersion(),expectedEligible))throw new TemplateStore.Stale();
            if(decision.action()==TemplateDecision.Action.APPROVE || decision.action()==TemplateDecision.Action.ACTIVATE)
                requireBase(s,t.version(decision.version()).definition());
            t.decide(decision);s.save(t);return false;
        });
    }
    private static void requireBase(TemplateStore.Session s,TemplateDefinition d) {
        var base=s.base(d.baseQuestionId(),d.baseRevisionId()).orElseThrow(() -> new IllegalArgumentException("Base inexistente"));
        if(!base.eligible() || !base.fingerprint().equals(d.baseFingerprint()) || !base.content().classification().equals(d.classification()))
            throw new IllegalArgumentException("Base não elegível ou incompatível com a definição");
    }
}

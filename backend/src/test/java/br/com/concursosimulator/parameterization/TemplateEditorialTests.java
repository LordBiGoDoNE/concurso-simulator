package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.function.Function;
import static br.com.concursosimulator.parameterization.ParameterFixtures.*;
import static org.assertj.core.api.Assertions.*;

class TemplateEditorialTests {
    @Test void submissionApprovalSuccessionWithdrawalAndExplicitActivationKeepHistorySeparate() {
        var v=TemplateUseCases.preview(definition(Family.PERCENTAGE));var family=new TemplateFamily(v);
        assertThat(family.eligible()).isEmpty();
        assertThatThrownBy(() -> family.decide(decision(v,TemplateDecision.Action.APPROVE))).isInstanceOf(IllegalStateException.class);
        family.decide(decision(v,TemplateDecision.Action.SUBMIT));family.decide(decision(v,TemplateDecision.Action.APPROVE));
        var v2=TemplateUseCases.preview(version(v.definition(),2));family.append(v2);
        family.decide(decision(v2,TemplateDecision.Action.SUBMIT));family.decide(decision(v2,TemplateDecision.Action.APPROVE));
        assertThat(family.eligibleVersion()).isEqualTo(2);
        family.decide(decision(v,TemplateDecision.Action.WITHDRAW));assertThat(family.eligibleVersion()).isEqualTo(2);
        family.decide(decision(v2,TemplateDecision.Action.WITHDRAW));assertThat(family.eligible()).isEmpty();
        assertThat(family.status(1)).isEqualTo(TemplateFamily.Status.APPROVED);
        family.decide(decision(v,TemplateDecision.Action.ACTIVATE));assertThat(family.eligibleVersion()).isEqualTo(1);
        var restored=TemplateFamily.restore(family.versions(),family.decisions());
        assertThat(restored.decisions()).isEqualTo(family.decisions());assertThat(restored.eligibleVersion()).isEqualTo(1);
    }
    @Test void exactManifestChecklistAndOperationPayloadAreMandatory() {
        var v=TemplateUseCases.preview(definition(Family.DIRECT_PROPORTION));var family=new TemplateFamily(v);
        var submit=decision(v,TemplateDecision.Action.SUBMIT);family.decide(submit);family.decide(submit);
        assertThat(family.sequence()).isEqualTo(1);
        assertThatThrownBy(() -> TemplateFamily.restore(List.of(v),List.of(submit,submit))).isInstanceOf(IllegalArgumentException.class);
        var approve=decision(v,TemplateDecision.Action.APPROVE);
        var wrong=new TemplateDecision(approve.operationId(),approve.templateId(),approve.version(),approve.action(),"0".repeat(64),approve.domainDigest(),approve.manifestDigest(),approve.baseRevisionId(),approve.baseFingerprint(),approve.responsible(),approve.at(),approve.checklist(),approve.rationale());
        assertThatThrownBy(() -> family.decide(wrong)).isInstanceOf(IllegalArgumentException.class);
        var incomplete=new TemplateDecision(approve.operationId(),approve.templateId(),approve.version(),approve.action(),approve.definitionDigest(),approve.domainDigest(),approve.manifestDigest(),approve.baseRevisionId(),approve.baseFingerprint(),approve.responsible(),approve.at(),Set.of(),approve.rationale());
        assertThatThrownBy(() -> family.decide(incomplete)).isInstanceOf(IllegalArgumentException.class);
        family.decide(approve);
        assertThatThrownBy(() -> family.decide(incomplete)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> family.append(v)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void useCasesPreserveIdempotenceAndDoNotRestoreWithdrawnTemplateOnRegistration() {
        var store=new Memory();var d=store.base();var uc=new TemplateUseCases(store);
        var registered=uc.register(d,0);assertThat(registered.replayed()).isFalse();
        assertThat(uc.register(d,0).replayed()).isTrue();var v=registered.version();
        var submit=decision(v,TemplateDecision.Action.SUBMIT);uc.decide(submit,null,0);
        var approve=decision(v,TemplateDecision.Action.APPROVE);uc.decide(approve,null,1);
        assertThat(uc.decide(approve,null,1)).isTrue();
        assertThatThrownBy(() -> uc.decide(decision(v,TemplateDecision.Action.WITHDRAW),1,1)).isInstanceOf(TemplateStore.Stale.class);
        uc.decide(decision(v,TemplateDecision.Action.WITHDRAW),1,2);
        uc.register(d,0);assertThat(store.data.get(d.id()).eligible()).isEmpty();
        store.eligible=false;
        assertThatThrownBy(() -> uc.decide(decision(v,TemplateDecision.Action.ACTIVATE),null,3)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.data.get(d.id()).sequence()).isEqualTo(3);
    }
    @Test void validationAndPreviewHaveNoStoreEffectsAndUnknownFailuresAreNotIdempotence() {
        var store=new Memory();var d=store.base();var preview=TemplateUseCases.preview(d);
        assertThat(store.data).isEmpty();assertThat(preview.manifest().entries()).hasSize(20);
        store.eligible=false;
        assertThatThrownBy(() -> new TemplateUseCases(store).register(d,0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.data).isEmpty();
        TemplateStore unavailable=new TemplateStore(){public <T>T transaction(Function<Session,T> action){throw new IllegalStateException("Indisponível");}};
        assertThatThrownBy(() -> new TemplateUseCases(unavailable).register(d,0)).isInstanceOf(IllegalStateException.class).hasMessage("Indisponível");
    }
    static final class Memory implements TemplateStore {
        Map<UUID,TemplateFamily> data=new HashMap<>();TemplateDefinition definition;QuestionContent content;boolean eligible=true;
        TemplateDefinition base() {
            var old=br.com.concursosimulator.questionbank.QuestionFixtures.content("Base matemática sintética");var initial=definition(Family.PERCENTAGE);
            content=new QuestionContent(old.statement(),old.alternatives(),old.correctAlternativeId(),old.resolution(),initial.classification(),old.references(),old.provenance());
            definition=new TemplateDefinition(initial.id(),1,initial.family(),initial.generatorVersion(),initial.baseQuestionId(),initial.baseRevisionId(),content.fingerprint(),initial.classification(),initial.references(),initial.provenance());return definition;
        }
        public <T>T transaction(Function<Session,T> action) {
            var copy=new HashMap<UUID,TemplateFamily>();data.forEach((id,t) -> copy.put(id,TemplateFamily.restore(t.versions(),t.decisions())));
            var result=action.apply(new Session(){
                public Optional<TemplateFamily> loadForUpdate(UUID id){return Optional.ofNullable(copy.get(id));}
                public Optional<TemplateFamily> findBaseForUpdate(UUID id){return copy.values().stream().filter(t -> t.baseQuestionId().equals(id)).findFirst();}
                public Optional<BaseRevision> base(UUID q,UUID r){return q.equals(definition.baseQuestionId()) && r.equals(definition.baseRevisionId())?Optional.of(new BaseRevision(q,r,content.fingerprint(),content,eligible)):Optional.empty();}
                public void insert(TemplateFamily t){copy.put(t.id(),t);}
                public void save(TemplateFamily t){copy.put(t.id(),t);}
            });data=copy;return result;
        }
    }
}

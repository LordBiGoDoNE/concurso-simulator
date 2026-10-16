package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.parameterization.ParameterFixtures;
import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

class ParameterizedAttemptTests {
    static AttemptQuestion base(Family family) {
        var original=question(1); var c=original.content(); var d=ParameterFixtures.definition(family);
        var math=new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                d.classification(),d.references(),d.provenance());
        return new AttemptQuestion(original.questionId(),original.revisionId(),math.fingerprint(),math);
    }
    static TemplateVersion template(AttemptQuestion q,Family family) {
        var c=q.content();
        var d=new TemplateDefinition(UUID.randomUUID(),1,family,NumericGenerator.VERSION,q.questionId(),q.revisionId(),
                q.fingerprint(),c.classification(),c.references(),c.provenance());
        return new TemplateVersion(d,VariantManifest.create(d));
    }
    static Composition math(int quantity,boolean enabled) {
        return new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,
                Map.of(Discipline.MATHEMATICS_LOGIC,new Composition.Portion(quantity,Set.of())),enabled);
    }
    static AttemptUseCases cases(AttemptUseCasesTests.Memory s) {
        return new AttemptUseCases(s,Clock.fixed(NOW,ZoneOffset.UTC),UUID::randomUUID,() -> new Random(8));
    }
    @Test void selectedBaseIdentitiesStayDistinctAndMissingTemplateStaysFixed() {
        var s=new AttemptUseCasesTests.Memory(); var a=base(Family.DIRECT_PROPORTION); var b=base(Family.PERCENTAGE);
        s.pool=List.of(a,b); s.templates=List.of(template(a,Family.DIRECT_PROPORTION));
        var uc=cases(s); var op=UUID.randomUUID(); var first=uc.create(ACCOUNT,op,math(2,true));
        var captured=s.attempts.get(first.attempt().id());
        assertThat(captured.items()).extracting(i -> i.question().questionId()).containsExactlyInAnyOrder(a.questionId(),b.questionId());
        assertThat(captured.items().stream().filter(i -> i.question().source().kind()==QuestionSource.Kind.PARAMETERIZED)).hasSize(1);
        assertThat(captured.items().stream().filter(i -> i.question().questionId().equals(b.questionId())).findFirst().orElseThrow().question()).isEqualTo(b);
        var generated=captured.items().stream().filter(i -> !i.parameters().isEmpty()).findFirst().orElseThrow();
        assertThat(generated.question().revisionId()).isEqualTo(a.revisionId());
        assertThat(generated.question().fingerprint()).isNotEqualTo(a.fingerprint());
        assertThat(generated.question().source().capture().baseFingerprint()).isEqualTo(a.fingerprint());
        assertThat(generated.question().content().alternatives()).extracting(QuestionContent.Alternative::id)
                .doesNotContainAnyElementsOf(a.content().alternatives().stream().map(QuestionContent.Alternative::id).toList());
        s.templatesUnavailable=true;
        assertThat(uc.create(ACCOUNT,op,math(2,true)).attempt()).isEqualTo(first.attempt());
        assertThat(s.selections).isEqualTo(1);
        assertThatThrownBy(() -> uc.create(ACCOUNT,op,math(2,false))).isInstanceOf(Attempt.Conflict.class);
    }
    @Test void falseNeverCapturesTemplatesAndTrueWithNoneRemainsFixed() {
        var s=new AttemptUseCasesTests.Memory(); s.pool=List.of(base(Family.PERCENTAGE)); s.templatesUnavailable=true;
        var uc=cases(s); var op=UUID.randomUUID(); var fixed=uc.create(ACCOUNT,op,math(1,false));
        var absent=new Composition(math(1,false).role(),math(1,false).disciplines());
        assertThat(uc.create(ACCOUNT,op,absent).attempt()).isEqualTo(fixed.attempt());
        s.templatesUnavailable=false;
        var fallback=uc.create(ACCOUNT,UUID.randomUUID(),math(1,true));
        assertThat(s.attempts.get(fallback.attempt().id()).items().getFirst().question()).isEqualTo(s.pool.getFirst());
    }
    @Test void eachDomainIndexMapsOnceToOneTupleAndOpaqueIdsDoNotAlterNumericOutput() {
        for (var family:Family.values()) {
            var q=base(family); var v=template(q,family); var seen=new HashSet<Parameters>();
            for (int index=0;index<family.domain().size();index++) {
                final int value=index;
                var rng=new Random() { @Override public int nextInt(int bound) {
                    assertThat(bound).isEqualTo(family.domain().size()); return value;
                }};
                var generated=AttemptMaterializer.materialize(List.of(q),List.of(v),rng,UUID::randomUUID).getFirst();
                var capture=generated.source().capture(); seen.add(capture.parameters());
                assertThat(capture.parameters()).isEqualTo(family.domain().get(index));
                var expected=NumericGenerator.render(v.definition(),capture.parameters());
                assertThat(capture.outputDigest()).isEqualTo(expected.digest());
                assertThat(generated.content().statement()).isEqualTo(expected.statement());
                assertThat(generated.content().resolution()).isEqualTo(expected.resolution());
            }
            assertThat(seen).hasSize(family.domain().size());
        }
    }
    @Test void eligibleButMismatchedTemplateFailsWholeCreationInsteadOfFallback() {
        var s=new AttemptUseCasesTests.Memory(); var q=base(Family.PERCENTAGE); var v=template(q,Family.PERCENTAGE);
        s.templates=List.of(v); var different=base(Family.PERCENTAGE);
        s.pool=List.of(new AttemptQuestion(q.questionId(),UUID.randomUUID(),different.fingerprint(),different.content()));
        assertThatThrownBy(() -> cases(s).create(ACCOUNT,UUID.randomUUID(),math(1,true))).isInstanceOf(IllegalStateException.class);
        assertThat(s.attempts).isEmpty(); assertThat(s.commands).isEmpty();
        assertThatThrownBy(() -> AttemptMaterializer.materialize(List.of(q),List.of(v,v),new Random(),UUID::randomUUID))
                .isInstanceOf(IllegalStateException.class);
        var bad=new VariantManifest(v.manifest().definitionDigest(),v.manifest().domainDigest(),v.manifest().generatorVersion(),
                List.of(new VariantManifest.Entry(v.manifest().entries().getFirst().parameters(),"0".repeat(64))));
        assertThatThrownBy(() -> new TemplateVersion(v.definition(),bad)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void correctionHistoryAndReplayUseOnlyFrozenContentAndKeepAccessRules() {
        var s=new AttemptUseCasesTests.Memory(); var q=base(Family.SIMPLE_INTEREST);
        s.pool=List.of(q); s.templates=List.of(template(q,Family.SIMPLE_INTEREST)); var uc=cases(s);
        var created=uc.create(ACCOUNT,UUID.randomUUID(),math(1,true)).attempt();
        var item=s.attempts.get(created.id()).items().getFirst();
        // Nenhuma captura de catálogo/gerador pode ocorrer daqui em diante.
        s.templatesUnavailable=true; s.pool=List.of(); s.templates=List.of();
        assertThat(uc.read(ACCOUNT,created.id())).isEqualTo(created);
        uc.answer(ACCOUNT,created.id(),UUID.randomUUID(),0,item.id(),item.question().content().correctAlternativeId());
        var finishOp=UUID.randomUUID(); var finish=uc.finish(ACCOUNT,created.id(),finishOp,1);
        assertThat(finish.result().correct()).isEqualTo(1);
        assertThat(finish.result().items().getFirst().resolution()).isEqualTo(item.question().content().resolution());
        assertThat(uc.finish(ACCOUNT,created.id(),finishOp,1).result()).isEqualTo(finish.result());
        assertThat(uc.history(ACCOUNT,new AttemptStore.HistoryQuery(0,10,true))).hasSize(1);
        assertThatThrownBy(() -> uc.read(VISITOR,created.id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> new Attempt.Item(item.id(),item.question(),item.optionOrder(),Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void validTupleAndDigestFromAnotherOutputCannotBeAttachedToCapturedContent() {
        var q=base(Family.PERCENTAGE);var v=template(q,Family.PERCENTAGE);
        var first=AttemptMaterializer.materialize(List.of(q),List.of(v),new Random(){
            @Override public int nextInt(int bound){return 0;}
        },UUID::randomUUID).getFirst();
        var other=v.definition().family().domain().get(1);var otherOutput=NumericGenerator.render(v.definition(),other);
        v.manifest().verifyOutput(otherOutput); // O par trocado é válido, não digest zerado/inexistente.
        var c=first.source().capture();
        var swapped=new QuestionSource.Capture(c.templateId(),c.templateVersion(),c.definitionDigest(),c.domainDigest(),
                c.manifestDigest(),otherOutput.digest(),c.generatorVersion(),c.baseRevisionId(),c.baseFingerprint(),c.renderedFingerprint(),other);
        assertThatThrownBy(()->new AttemptQuestion(first.questionId(),first.revisionId(),first.fingerprint(),first.content(),
                new QuestionSource(QuestionSource.Kind.PARAMETERIZED,swapped))).isInstanceOf(IllegalArgumentException.class);
    }
}

package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.TextContent;
import org.junit.jupiter.api.Test;
import java.util.*;
import static br.com.concursosimulator.parameterization.ParameterFixtures.*;
import static org.assertj.core.api.Assertions.*;

class VariantManifestTests {
    @Test void all111TuplesSatisfyAnIndependentOracleAndManifestsAreDeterministic() {
        int total=0;
        for(var family:Family.values()) {
            var d=definition(family);var manifest=VariantManifest.create(d);
            assertThat(manifest).isEqualTo(VariantManifest.create(d));
            manifest.verify(d);total+=manifest.entries().size();
            for(var entry:manifest.entries()) {
                var variant=NumericGenerator.render(d,entry.parameters());
                NumericOracle.verify(variant);manifest.verifyOutput(variant);
            }
            assertThat(manifest.digest()).matches("[0-9a-f]{64}");
            assertThatThrownBy(() -> manifest.verify(version(d,2))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(total).isEqualTo(111);
    }
    @Test void oracleRejectsEachWrongOptionAcrossTheFullDomainRatherThanTrustingGeneratorIndex() {
        for(var family:Family.values()) {
            var d=definition(family);
            for(var p:family.domain()) {
                var v=NumericGenerator.render(d,p);
                for(int wrong=1;wrong<4;wrong++) {
                    var changed=new GeneratedVariant(d,p,v.statement(),v.alternatives(),wrong,v.resolution());
                    assertThatThrownBy(() -> NumericOracle.verify(changed)).isInstanceOf(IllegalArgumentException.class);
                }
            }
        }
    }
    @Test void missingRepeatedReorderedOrAlteredTupleBlocksTheEntireManifest() {
        var d=definition(Family.DIRECT_PROPORTION);
        var outputs=d.family().domain().stream().map(p -> NumericGenerator.render(d,p)).toList();
        assertThatThrownBy(() -> VariantManifest.fromOutputs(d,outputs.subList(0,63))).isInstanceOf(IllegalArgumentException.class);
        var changed=new ArrayList<>(outputs);changed.set(1,changed.getFirst());
        assertThatThrownBy(() -> VariantManifest.fromOutputs(d,changed)).isInstanceOf(IllegalArgumentException.class);
        Collections.swap(changed,0,63);
        assertThatThrownBy(() -> VariantManifest.fromOutputs(d,changed)).isInstanceOf(IllegalArgumentException.class);
        var first=outputs.getFirst();var falseText=new GeneratedVariant(d,first.parameters(),first.statement(),first.alternatives(),0,new TextContent("Resolução adulterada",List.of()));
        var tampered=new ArrayList<>(outputs);tampered.set(0,falseText);
        assertThatThrownBy(() -> VariantManifest.fromOutputs(d,tampered)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VariantManifest.create(d).verifyOutput(falseText)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void storedManifestCannotChangeDigestsParametersOrEntriesAndPassVerification() {
        var d=definition(Family.PERCENTAGE);var m=VariantManifest.create(d);var list=new ArrayList<>(m.entries());
        list.set(0,new VariantManifest.Entry(list.getFirst().parameters(),"0".repeat(64)));
        var tampered=new VariantManifest(m.definitionDigest(),m.domainDigest(),m.generatorVersion(),list);
        assertThat(tampered.digest()).isNotEqualTo(m.digest());
        assertThatThrownBy(() -> tampered.verify(d)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VariantManifest(m.definitionDigest(),m.domainDigest(),m.generatorVersion(),List.of(m.entries().getFirst(),m.entries().getFirst())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> m.entries().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}

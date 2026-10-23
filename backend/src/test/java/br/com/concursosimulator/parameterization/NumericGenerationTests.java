package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static br.com.concursosimulator.parameterization.ParameterFixtures.*;
import static org.assertj.core.api.Assertions.*;

class NumericGenerationTests {
    @Test void definitionsRejectUnsupportedVersionsAndBindDigestToBaseAndMetadata() {
        var d=definition(Family.PERCENTAGE);
        assertThatThrownBy(() -> version(d,0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TemplateDefinition(d.id(),1,d.family(),"script:eval",d.baseQuestionId(),d.baseRevisionId(),d.baseFingerprint(),d.classification(),d.references(),d.provenance()))
                .isInstanceOf(IllegalArgumentException.class);
        var changed=new TemplateDefinition(d.id(),1,d.family(),d.generatorVersion(),d.baseQuestionId(),UUID.randomUUID(),d.baseFingerprint(),d.classification(),d.references(),d.provenance());
        assertThat(changed.digest()).isNotEqualTo(d.digest());
        assertThatThrownBy(() -> new GeneratedVariant(d,d.family().domain().getFirst(),new br.com.concursosimulator.questionbank.domain.TextContent("Texto",List.of()),
                List.of(BigInteger.ONE,BigInteger.ONE,BigInteger.TWO,BigInteger.TEN),0,new br.com.concursosimulator.questionbank.domain.TextContent("Passos",List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void closedDomainsAreCompleteDistinctImmutableAndRejectExtraMissingOrInvalidValues() {
        assertThat(Family.DIRECT_PROPORTION.domain()).hasSize(64).doesNotHaveDuplicates();
        assertThat(Family.PERCENTAGE.domain()).hasSize(20).doesNotHaveDuplicates();
        assertThat(Family.SIMPLE_INTEREST.domain()).hasSize(27).doesNotHaveDuplicates();
        assertThatThrownBy(() -> new Parameters(Family.PERCENTAGE,Map.of("v",100,"p",50))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Parameters(Family.PERCENTAGE,Map.of("v",100,"p",5,"answer",5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Parameters(Family.PERCENTAGE,Map.of("p",5))).isInstanceOf(IllegalArgumentException.class);
        var source=new HashMap<>(Map.of("v",100,"p",5));var p=new Parameters(Family.PERCENTAGE,source);source.put("v",999);
        assertThat(p.value("v")).isEqualTo(100);
        assertThatThrownBy(() -> p.values().put("v",200)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> NumericGenerator.render(definition(Family.DIRECT_PROPORTION),p)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void referenceProblemsHaveSpecificValuesUnitsAndExplanations() {
        var interest=NumericGenerator.render(definition(Family.SIMPLE_INTEREST),new Parameters(Family.SIMPLE_INTEREST,Map.of("c",2000,"i",2,"t",3)));
        assertThat(interest.correctValue()).isEqualTo(BigInteger.valueOf(120));
        assertThat(interest.statement().text()).contains("2% ao mês","3 meses","juros");
        assertThat(interest.resolution().text()).contains("2000 × 2 × 3 ÷ 100 = 120","R$ 2120,00");
        var direct=NumericGenerator.render(definition(Family.DIRECT_PROPORTION),new Parameters(Family.DIRECT_PROPORTION,Map.of("a",3,"b",8,"u",5)));
        assertThat(direct.correctValue()).isEqualTo(BigInteger.valueOf(40));
        assertThat(direct.statement().text()).contains("R$ 15,00","8 cadernos");
        var percent=NumericGenerator.render(definition(Family.PERCENTAGE),new Parameters(Family.PERCENTAGE,Map.of("v",400,"p",15)));
        assertThat(percent.correctValue()).isEqualTo(BigInteger.valueOf(60));
    }
    @Test void allTuplesHaveFourPositiveDistinctAlternativesAndRepeatExactlyWithoutRandomIds() {
        for(var family:Family.values()) {
            var d=definition(family);
            for(var p:family.domain()) {
                var v=NumericGenerator.render(d,p);
                assertThat(v.alternatives()).hasSize(4).doesNotHaveDuplicates().allMatch(x -> x.signum()>0);
                assertThat(v).isEqualTo(NumericGenerator.render(d,p));
                assertThat(v.digest()).matches("[0-9a-f]{64}");
                assertThat(v.statement().resources()).isEmpty();
            }
            assertThat(version(d,2).digest()).isNotEqualTo(d.digest());
        }
        assertThat(new Parameters(Family.PERCENTAGE,new LinkedHashMap<>(Map.of("p",5,"v",100))).key())
                .isEqualTo(new Parameters(Family.PERCENTAGE,Map.of("v",100,"p",5)).key());
    }
}

package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.net.URI;
import java.util.*;

/** Somente conteúdo sintético. Não representa aprovação editorial de famílias reais. */
public final class ParameterFixtures {
    private ParameterFixtures() {}
    public static TemplateDefinition definition(Family family) {
        return new TemplateDefinition(UUID.randomUUID(),1,family,NumericGenerator.VERSION,UUID.randomUUID(),UUID.randomUUID(),"a".repeat(64),
                new QuestionClassification(Discipline.MATHEMATICS_LOGIC,family.topic(),Set.of(TargetRole.values())),
                List.of(new StudyReference("Referência sintética",URI.create("https://example.org/matematica"),null)),
                new Provenance("Fixture autoral","fixtures/template.json","fixtures/resolution.json","b".repeat(64),"c".repeat(64)));
    }
    public static TemplateDefinition version(TemplateDefinition d,int number) {
        return new TemplateDefinition(d.id(),number,d.family(),d.generatorVersion(),d.baseQuestionId(),d.baseRevisionId(),d.baseFingerprint(),d.classification(),d.references(),d.provenance());
    }
    public static TemplateDecision decision(TemplateVersion v,TemplateDecision.Action action) {
        var d=v.definition();return new TemplateDecision(UUID.randomUUID(),d.id(),d.version(),action,d.digest(),d.family().domainDigest(),
                v.manifest().digest(),d.baseRevisionId(),d.baseFingerprint(),"Fixture",java.time.Instant.parse("2026-10-15T00:00:00Z"),
                Set.of(TemplateDecision.Check.values()),"Decisão sintética de teste, não aprovação real");
    }
}

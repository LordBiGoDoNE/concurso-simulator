package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/** A seleção de identidades já ocorreu; cada base recebe no máximo uma tupla. */
public final class AttemptMaterializer {
    private AttemptMaterializer() {}
    public static List<AttemptQuestion> materialize(List<AttemptQuestion> selected, List<TemplateVersion> eligible,
                                                   RandomGenerator random, Supplier<UUID> ids) {
        var byBase=new HashMap<UUID,TemplateVersion>();
        for (var version:eligible) {
            if (byBase.put(version.definition().baseQuestionId(),version)!=null)
                throw new IllegalStateException("Template elegível duplicado");
        }
        return selected.stream().map(q -> {
            var version=byBase.get(q.questionId()); if (version==null) return q;
            var d=version.definition();
            if (!d.baseRevisionId().equals(q.revisionId()) || !d.baseFingerprint().equals(q.fingerprint())
                    || !d.classification().equals(q.content().classification()) || q.source().kind()!=QuestionSource.Kind.FIXED)
                throw new IllegalStateException("Template/base divergentes");
            var domain=d.family().domain(); var p=domain.get(random.nextInt(domain.size()));
            var output=NumericGenerator.render(d,p); version.manifest().verifyOutput(output);
            var options=output.alternatives().stream().map(v -> new QuestionContent.Alternative(ids.get(),
                    new TextContent("R$ "+v+",00",List.of()))).toList();
            var content=new QuestionContent(output.statement(),options,options.get(output.correctIndex()).id(),
                    output.resolution(),d.classification(),d.references(),d.provenance());
            var capture=new QuestionSource.Capture(d.id(),d.version(),d.digest(),d.family().domainDigest(),
                    version.manifest().digest(),output.digest(),d.generatorVersion(),q.revisionId(),q.fingerprint(),content.fingerprint(),p);
            return new AttemptQuestion(q.questionId(),q.revisionId(),content.fingerprint(),content,
                    new QuestionSource(QuestionSource.Kind.PARAMETERIZED,capture));
        }).toList();
    }
}

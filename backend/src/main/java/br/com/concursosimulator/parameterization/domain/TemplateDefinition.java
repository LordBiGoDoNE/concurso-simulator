package br.com.concursosimulator.parameterization.domain;

import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;

/** Definição imutável; identidade da família é id, e version identifica uma revisão dela. */
public record TemplateDefinition(UUID id,int version,Family family,String generatorVersion,
                                 UUID baseQuestionId,UUID baseRevisionId,String baseFingerprint,
                                 QuestionClassification classification,List<StudyReference> references,Provenance provenance) {
    public TemplateDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(family); Objects.requireNonNull(baseQuestionId);
        Objects.requireNonNull(baseRevisionId); CanonicalDigest.hash(baseFingerprint);
        Objects.requireNonNull(classification); Objects.requireNonNull(provenance); references=List.copyOf(references);
        if(version<1 || !NumericGenerator.VERSION.equals(generatorVersion) || references.isEmpty()
                || references.size()>10 || classification.discipline()!=Discipline.MATHEMATICS_LOGIC
                || classification.topic()!=family.topic() || !classification.roles().equals(Set.of(TargetRole.values())))
            throw new IllegalArgumentException("Definição incompatível com as três famílias numeric-v1");
    }
    public String digest() {
        var f=new ArrayList<String>(List.of("parameter-template/v1",id.toString(),Integer.toString(version),family.name(),
                generatorVersion,baseQuestionId.toString(),baseRevisionId.toString(),baseFingerprint,family.domainDigest(),
                classification.discipline().id(),classification.topic().id()));
        classification.roles().stream().map(TargetRole::id).sorted().forEach(f::add);
        f.add(Integer.toString(references.size()));
        for(var r:references) {
            f.add(r.name());f.add(r.url().toString()); f.add(Boolean.toString(r.legalBasis()!=null));
            if(r.legalBasis()!=null) {f.add(r.legalBasis().provision());f.add(r.legalBasis().checkedOn().toString());}
        }
        f.addAll(List.of(provenance.author(),provenance.contentPath(),provenance.answerPath(),provenance.contentSha256(),provenance.answerSha256()));
        return CanonicalDigest.of(f);
    }
}

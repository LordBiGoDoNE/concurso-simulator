package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.parameterization.domain.Parameters;
import java.util.*;

/** Captura privada; restauração não consulta templates nem executa geradores. */
public record QuestionSource(Kind kind, Capture capture) {
    public enum Kind { FIXED, PARAMETERIZED }
    public record Capture(UUID templateId, int templateVersion, String definitionDigest, String domainDigest,
                          String manifestDigest, String outputDigest, String generatorVersion,
                          UUID baseRevisionId, String baseFingerprint, String renderedFingerprint, Parameters parameters) {
        public Capture {
            Objects.requireNonNull(templateId); Objects.requireNonNull(baseRevisionId); Objects.requireNonNull(parameters);
            if (templateVersion < 1 || generatorVersion == null || generatorVersion.isBlank())
                throw new IllegalArgumentException("Captura inválida");
            for (var hash : List.of(definitionDigest,domainDigest,manifestDigest,outputDigest,baseFingerprint,renderedFingerprint))
                if (!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Digest inválido");
            if (!domainDigest.equals(parameters.family().domainDigest())) throw new IllegalArgumentException("Domínio divergente");
        }
        public Map<String,String> parameterStrings() {
            var values=new TreeMap<String,String>(); parameters.values().forEach((k,v) -> values.put(k,v.toString()));
            return Map.copyOf(values);
        }
    }
    public QuestionSource {
        Objects.requireNonNull(kind);
        if ((kind==Kind.FIXED)!=(capture==null)) throw new IllegalArgumentException("Fonte incompleta");
    }
    public static QuestionSource fixed() { return new QuestionSource(Kind.FIXED,null); }
}

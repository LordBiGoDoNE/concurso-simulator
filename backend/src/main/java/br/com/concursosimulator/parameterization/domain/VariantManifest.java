package br.com.concursosimulator.parameterization.domain;

import java.util.*;

/** Manifesto de todas as saídas, na ordem canônica do domínio, anterior a UUIDs de alternativas. */
public record VariantManifest(String definitionDigest,String domainDigest,String generatorVersion,List<Entry> entries) {
    public record Entry(Parameters parameters,String outputDigest) {
        public Entry { Objects.requireNonNull(parameters); CanonicalDigest.hash(outputDigest); }
    }
    public VariantManifest {
        CanonicalDigest.hash(definitionDigest); CanonicalDigest.hash(domainDigest);
        if(!NumericGenerator.VERSION.equals(generatorVersion)) throw new IllegalArgumentException("Gerador desconhecido");
        entries=List.copyOf(entries);
        if(entries.isEmpty() || entries.size()>64 || entries.stream().map(Entry::parameters).distinct().count()!=entries.size())
            throw new IllegalArgumentException("Manifesto vazio, repetido ou acima do limite");
    }
    public static VariantManifest create(TemplateDefinition definition) {
        return fromOutputs(definition,definition.family().domain().stream().map(p -> NumericGenerator.render(definition,p)).toList());
    }
    public static VariantManifest fromOutputs(TemplateDefinition definition,List<GeneratedVariant> outputs) {
        var domain=definition.family().domain();
        if(outputs.size()!=domain.size()) throw new IllegalArgumentException("Domínio incompleto");
        var entries=new ArrayList<Entry>();
        for(int i=0;i<domain.size();i++) {
            var v=Objects.requireNonNull(outputs.get(i));
            if(!definition.equals(v.definition()) || !domain.get(i).equals(v.parameters()))
                throw new IllegalArgumentException("Definição, domínio ou ordem divergente");
            NumericOracle.verify(v);
            if(!v.equals(NumericGenerator.render(definition,v.parameters())))
                throw new IllegalArgumentException("Saída divergente do gerador versionado");
            entries.add(new Entry(v.parameters(),v.digest()));
        }
        return new VariantManifest(definition.digest(),definition.family().domainDigest(),definition.generatorVersion(),entries);
    }
    public void verify(TemplateDefinition definition) {
        if(!equals(create(definition))) throw new IllegalArgumentException("Manifesto não corresponde à definição/saídas exatas");
    }
    public String digest() {
        var fields=new ArrayList<String>(List.of("parameter-manifest/v1",definitionDigest,domainDigest,generatorVersion,Integer.toString(entries.size())));
        entries.forEach(e -> { fields.add(e.parameters().key()); fields.add(e.outputDigest()); });
        return CanonicalDigest.of(fields);
    }
    /** Divergência é integridade, nunca autorização para trocar a instância por uma fixa. */
    public void verifyOutput(GeneratedVariant variant) {
        if(!definitionDigest.equals(variant.definition().digest()) || !domainDigest.equals(variant.parameters().family().domainDigest())
                || entries.stream().noneMatch(e -> e.parameters().equals(variant.parameters()) && e.outputDigest().equals(variant.digest())))
            throw new IllegalArgumentException("Instância diverge do manifesto");
        NumericOracle.verify(variant);
    }
}

package br.com.concursosimulator.parameterization.domain;

import br.com.concursosimulator.questionbank.domain.TextContent;
import java.math.BigInteger;
import java.util.*;

/** Saída privada anterior à atribuição de UUIDs opacos e embaralhamento da tentativa. */
public record GeneratedVariant(TemplateDefinition definition,Parameters parameters,TextContent statement,
                               List<BigInteger> alternatives,int correctIndex,TextContent resolution) {
    public GeneratedVariant {
        Objects.requireNonNull(definition); Objects.requireNonNull(parameters); Objects.requireNonNull(statement);
        Objects.requireNonNull(resolution); alternatives=List.copyOf(alternatives);
        if(parameters.family()!=definition.family() || alternatives.size()!=4 || new HashSet<>(alternatives).size()!=4
                || alternatives.stream().anyMatch(v -> v.signum()<=0) || correctIndex<0 || correctIndex>=4
                || !statement.resources().isEmpty() || !resolution.resources().isEmpty())
            throw new IllegalArgumentException("Instância numérica inválida");
    }
    public String digest() {
        return CapturedOutputDigest.of(definition.digest(),parameters,statement,alternatives,correctIndex,resolution);
    }
    public BigInteger correctValue() { return alternatives.get(correctIndex); }
}

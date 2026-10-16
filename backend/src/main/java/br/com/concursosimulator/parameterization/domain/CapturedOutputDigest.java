package br.com.concursosimulator.parameterization.domain;

import br.com.concursosimulator.questionbank.domain.*;
import java.math.BigInteger;
import java.util.*;

/** Canonização de saída v1; não resolve questões nem consulta geradores ou elegibilidade. */
public final class CapturedOutputDigest {
    private CapturedOutputDigest() {}
    static String of(String definitionDigest,Parameters parameters,TextContent statement,
                     List<BigInteger> alternatives,int correctIndex,TextContent resolution) {
        CanonicalDigest.hash(definitionDigest);
        var fields=new ArrayList<String>(List.of("parameter-output/v1",definitionDigest,parameters.key(),statement.text(),Integer.toString(correctIndex)));
        alternatives.forEach(v->fields.add(v.toString()));fields.add(resolution.text());return CanonicalDigest.of(fields);
    }
    public static String fromContent(String definitionDigest,Parameters parameters,QuestionContent content) {
        if (!content.statement().resources().isEmpty() || !content.resolution().resources().isEmpty()
                || content.alternatives().size()!=4) throw new IllegalArgumentException("Formato de saída capturada inválido");
        var values=new ArrayList<BigInteger>();int correctIndex=-1;
        for (int i=0;i<content.alternatives().size();i++) {
            var option=content.alternatives().get(i);var text=option.content().text();
            if (!option.content().resources().isEmpty() || !text.matches("R[$] [1-9][0-9]*,00"))
                throw new IllegalArgumentException("Alternativa capturada fora do formato v1");
            values.add(new BigInteger(text.substring(3,text.length()-3)));
            if (option.id().equals(content.correctAlternativeId())) correctIndex=i;
        }
        if (correctIndex<0) throw new IllegalArgumentException("Correção capturada ausente");
        return of(definitionDigest,parameters,content.statement(),values,correctIndex,content.resolution());
    }
}

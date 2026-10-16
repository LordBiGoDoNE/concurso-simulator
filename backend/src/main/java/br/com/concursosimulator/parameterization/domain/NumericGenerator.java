package br.com.concursosimulator.parameterization.domain;

import br.com.concursosimulator.questionbank.domain.TextContent;
import java.math.BigInteger;
import java.util.*;

/** Três cálculos explícitos e limitados. Mudança de texto/cálculo exige nova versão e manifesto. */
public final class NumericGenerator {
    public static final String VERSION="numeric-v1";
    private NumericGenerator() {}
    public static GeneratedVariant render(TemplateDefinition definition,Parameters p) {
        if(definition.family()!=p.family()) throw new IllegalArgumentException("Família divergente");
        String statement,resolution; List<Integer> values;
        switch(p.family()) {
            case DIRECT_PROPORTION -> {
                int a=p.value("a"),b=p.value("b"),u=p.value("u"),total=a*u,answer=b*u;
                statement=a+" cadernos iguais custam "+money(total)+". Mantido o preço por unidade, quanto custam "+b+" cadernos?";
                resolution="1. O preço de "+a+" cadernos é "+money(total)+". 2. Divida pelo número de unidades: "+total+" ÷ "+a+" = "+u+" reais por caderno. 3. Multiplique por "+b+": "+u+" × "+b+" = "+answer+". Resposta: "+money(answer)+". O custo é diretamente proporcional à quantidade; não some quantidades nem esqueça a divisão inicial.";
                values=List.of(answer,total,(a+b)*u,a*b*u);
            }
            case PERCENTAGE -> {
                int v=p.value("v"),percent=p.value("p"),answer=v*percent/100;
                statement="Quanto corresponde a "+percent+"% de "+money(v)+"?";
                resolution="1. "+percent+"% significa "+percent+" em cada 100. 2. Calcule "+v+" × "+percent+" ÷ 100 = "+answer+". Resposta: "+money(answer)+". A pergunta pede apenas a parte percentual, não o total após aumento ou o restante após desconto.";
                values=List.of(answer,v*percent,v+answer,v-answer);
            }
            case SIMPLE_INTEREST -> {
                int c=p.value("c"),i=p.value("i"),t=p.value("t"),answer=c*i*t/100;
                statement="Um capital de "+money(c)+" rende juros simples de "+i+"% ao mês por "+t+" meses. Qual é o valor dos juros?";
                resolution="1. Em juros simples, J = capital × taxa × tempo. 2. Converta a taxa mensal: "+i+"% = "+i+"/100. 3. Use o prazo em meses: "+c+" × "+i+" × "+t+" ÷ 100 = "+answer+". Juros: "+money(answer)+"; o montante seria "+money(c+answer)+" e não é o que foi pedido. A taxa não incide sobre juros acumulados.";
                values=List.of(answer,c+answer,c*i/100,c*i*t);
            }
            default -> throw new IllegalArgumentException("Família desconhecida");
        }
        return new GeneratedVariant(definition,p,new TextContent(statement,List.of()),values.stream().map(BigInteger::valueOf).toList(),0,new TextContent(resolution,List.of()));
    }
    private static String money(int value) { return "R$ "+value+",00"; }
}

package br.com.concursosimulator.parameterization.domain;

import java.math.BigInteger;

/** Relações aritméticas independentes: não chama o gerador nem reutiliza seus resultados intermediários. */
public final class NumericOracle {
    private NumericOracle() {}
    public static void verify(GeneratedVariant variant) {
        var p=variant.parameters(); var answer=variant.correctValue();
        boolean correct=switch(p.family()) {
            case DIRECT_PROPORTION -> answer.multiply(n(p.value("a"))).equals(
                    n(p.value("a")).multiply(n(p.value("u"))).multiply(n(p.value("b"))));
            case PERCENTAGE -> answer.multiply(n(100)).equals(n(p.value("v")).multiply(n(p.value("p"))));
            case SIMPLE_INTEREST -> answer.multiply(n(100)).equals(n(p.value("c")).multiply(n(p.value("i"))).multiply(n(p.value("t"))));
        };
        if(!correct) throw new IllegalArgumentException("Oráculo rejeitou a resposta da tupla");
    }
    private static BigInteger n(int value) { return BigInteger.valueOf(value); }
}

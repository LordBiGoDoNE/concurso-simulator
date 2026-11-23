package br.com.concursosimulator.shared.web;

import java.util.function.Supplier;

/**
 * Conversão de entrada HTTP em objetos de valor. A recusa do objeto de valor ({@link IllegalArgumentException})
 * só vira 400 aqui, no ponto de entrada; a mesma exceção vinda de dentro da aplicação ou da persistência
 * continua sendo erro interno.
 */
public final class RequestValues {
    private RequestValues() {}

    public static <T> T parse(Supplier<T> conversion) {
        try {
            return conversion.get();
        } catch (IllegalArgumentException refused) {
            throw new InvalidRequest("Valor recusado na entrada");
        }
    }

    public static void require(boolean condition) {
        if (!condition) throw new InvalidRequest("Pedido inválido");
    }

    public static InvalidRequest invalid() { return new InvalidRequest("Pedido inválido"); }
}

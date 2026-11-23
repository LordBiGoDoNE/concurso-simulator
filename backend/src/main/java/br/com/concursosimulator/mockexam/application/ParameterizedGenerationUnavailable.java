package br.com.concursosimulator.mockexam.application;

/** Geração de questões parametrizadas desligada no servidor; repetir sem variações numéricas funciona. */
public final class ParameterizedGenerationUnavailable extends RuntimeException {
    public ParameterizedGenerationUnavailable() {
        super("Geração temporariamente desabilitada");
    }
}

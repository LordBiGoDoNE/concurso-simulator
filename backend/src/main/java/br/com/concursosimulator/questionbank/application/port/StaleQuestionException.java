package br.com.concursosimulator.questionbank.application.port;

public final class StaleQuestionException extends RuntimeException {
    public StaleQuestionException() { super("Questão alterada desde a versão-base esperada"); }
}

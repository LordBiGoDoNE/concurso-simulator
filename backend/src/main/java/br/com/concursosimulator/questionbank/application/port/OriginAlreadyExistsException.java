package br.com.concursosimulator.questionbank.application.port;

/** Exclusivamente conflito da constraint de origem, exposto após rollback completo. */
public final class OriginAlreadyExistsException extends RuntimeException {
    public OriginAlreadyExistsException(Throwable cause) { super("Origem criada por operação concorrente",cause); }
}

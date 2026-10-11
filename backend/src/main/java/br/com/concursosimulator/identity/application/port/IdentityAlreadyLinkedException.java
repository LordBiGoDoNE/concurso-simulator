package br.com.concursosimulator.identity.application.port;

/** Conflito específico do vínculo, sem subject/token em sua mensagem. */
public final class IdentityAlreadyLinkedException extends RuntimeException {
    public IdentityAlreadyLinkedException(Throwable cause) {
        super("Identidade externa já vinculada", cause);
    }
}

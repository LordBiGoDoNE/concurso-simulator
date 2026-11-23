package br.com.concursosimulator.shared.web;

/** Entrada HTTP recusada pelo adaptador web; vira 400 {@code INVALID_REQUEST}, sem eco da mensagem. */
public final class InvalidRequest extends RuntimeException {
    public InvalidRequest(String message) { super(message, null, false, false); }
}

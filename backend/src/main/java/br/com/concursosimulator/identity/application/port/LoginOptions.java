package br.com.concursosimulator.identity.application.port;

/** Configuração pública necessária aos adaptadores, sem credenciais do provedor. */
public interface LoginOptions {
    boolean enabled();
    String frontendUrl();
}

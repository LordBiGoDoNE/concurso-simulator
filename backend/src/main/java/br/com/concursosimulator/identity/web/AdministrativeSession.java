package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Access;

/** Technical session boundary implemented by Spring Security, never by an application use case. */
public interface AdministrativeSession {
    void replace(Access access);
    String beginGoogleLink(Access access);
}

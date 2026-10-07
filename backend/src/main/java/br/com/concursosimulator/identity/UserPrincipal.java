package br.com.concursosimulator.identity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Identidade interna mínima; não contém claims, credenciais ou tokens Google. */
public record UserPrincipal(UUID id) implements Serializable, java.security.Principal {
    public UserPrincipal {
        Objects.requireNonNull(id, "O identificador interno é obrigatório");
    }

    @Override
    public String getName() { return id.toString(); }
}

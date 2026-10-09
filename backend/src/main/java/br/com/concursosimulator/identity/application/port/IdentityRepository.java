package br.com.concursosimulator.identity.application.port;

import br.com.concursosimulator.identity.domain.ExternalIdentity;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    Optional<UUID> findUserId(ExternalIdentity identity);

    /** Criar usuário/vínculo na unidade transacional do caso de uso. */
    void createUserWithIdentity(UUID userId, ExternalIdentity identity);
}

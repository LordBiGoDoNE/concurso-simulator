package br.com.concursosimulator.identity.application;

import br.com.concursosimulator.identity.application.port.IdentityAlreadyLinkedException;
import br.com.concursosimulator.identity.application.port.IdentityRepository;
import br.com.concursosimulator.identity.domain.ExternalIdentity;
import br.com.concursosimulator.shared.application.port.UnitOfWork;
import java.util.Objects;
import java.util.UUID;

public final class ResolveExternalIdentityUseCase {
    private final IdentityRepository identities;
    private final UnitOfWork transactions;

    public ResolveExternalIdentityUseCase(IdentityRepository identities, UnitOfWork transactions) {
        this.identities = Objects.requireNonNull(identities);
        this.transactions = Objects.requireNonNull(transactions);
    }

    /** Somente identidades de um fluxo de autenticação já verificado. */
    public UUID execute(ExternalIdentity identity) {
        Objects.requireNonNull(identity);
        try {
            return transactions.independently(() -> identities.findUserId(identity).orElseGet(() -> {
                UUID id = UUID.randomUUID();
                identities.createUserWithIdentity(id, identity);
                return id;
            }));
        } catch (IdentityAlreadyLinkedException conflict) {
            // A porta garante que a tentativa anterior já sofreu rollback.
            return transactions.independently(() -> identities.findUserId(identity).orElseThrow(() -> conflict));
        }
    }
}

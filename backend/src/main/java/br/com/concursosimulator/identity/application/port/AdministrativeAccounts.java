package br.com.concursosimulator.identity.application.port;

import br.com.concursosimulator.identity.domain.AdministrativeAccount;
import br.com.concursosimulator.identity.domain.ExternalIdentity;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface AdministrativeAccounts {
    <T> T atomic(Function<Session, T> action);
    Optional<AdministrativeAccount> current();
    boolean active(UUID user);
    interface Session {
        Optional<AdministrativeAccount> current();
        void save(AdministrativeAccount account);
        void createUser(UUID id);
        Optional<UUID> owner(ExternalIdentity identity);
        boolean hasGoogle(UUID owner);
        void attach(ExternalIdentity identity, UUID owner);
        void lockUsers(UUID first, UUID second);
        boolean hasHistory(UUID user);
        void deactivate(UUID user);
    }
}

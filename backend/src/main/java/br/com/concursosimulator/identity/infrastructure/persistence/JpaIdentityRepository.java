package br.com.concursosimulator.identity.infrastructure.persistence;

import br.com.concursosimulator.identity.application.port.IdentityAlreadyLinkedException;
import br.com.concursosimulator.identity.application.port.IdentityRepository;
import br.com.concursosimulator.identity.domain.ExternalIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaIdentityRepository implements IdentityRepository {
    private final EntityManager entities;

    public JpaIdentityRepository(EntityManager entities) { this.entities = entities; }

    @Override
    public Optional<UUID> findUserId(ExternalIdentity identity) {
        return Optional.ofNullable(entities.find(ExternalIdentityPersistenceEntity.class, key(identity)))
                .map(ExternalIdentityPersistenceEntity::userId);
    }

    @Override
    public void createUserWithIdentity(UUID userId, ExternalIdentity identity) {
        try {
            entities.persist(new UserPersistenceEntity(userId));
            entities.persist(new ExternalIdentityPersistenceEntity(key(identity), userId));
            entities.flush(); // Detectar o conflito dentro da unidade transacional, antes de retornar UUID.
        } catch (PersistenceException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "external_identity_key".equals(violation.getConstraintName())) {
                    throw new IdentityAlreadyLinkedException(exception);
                }
            }
            throw exception;
        }
    }

    private static ExternalIdentityPersistenceId key(ExternalIdentity identity) {
        return new ExternalIdentityPersistenceId(identity.provider(), identity.issuer(), identity.subject());
    }
}

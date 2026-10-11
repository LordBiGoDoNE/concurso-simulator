package br.com.concursosimulator.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "external_identity")
public class ExternalIdentityPersistenceEntity {
    @EmbeddedId
    private ExternalIdentityPersistenceId id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected ExternalIdentityPersistenceEntity() {}

    ExternalIdentityPersistenceEntity(ExternalIdentityPersistenceId id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    UUID userId() { return userId; }
}

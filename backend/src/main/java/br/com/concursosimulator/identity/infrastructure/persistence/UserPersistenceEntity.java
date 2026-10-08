package br.com.concursosimulator.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserPersistenceEntity {
    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt; // DEFAULT CURRENT_TIMESTAMP da migração, não do ORM.

    protected UserPersistenceEntity() {}

    UserPersistenceEntity(UUID id) { this.id = id; }
}

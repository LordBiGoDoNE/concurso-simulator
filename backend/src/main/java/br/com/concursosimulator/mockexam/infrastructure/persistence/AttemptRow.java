package br.com.concursosimulator.mockexam.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="me_attempt") @org.hibernate.annotations.DynamicUpdate
public class AttemptRow {
    @Id UUID id;
    @Column(name="owner_kind",nullable=false,length=7) String ownerKind;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    @Column(name="account_id") UUID accountId;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="finished_at") Instant finishedAt;
    @Column(name="expires_at") Instant expiresAt;
    @Version long version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") String payload;
    protected AttemptRow() {}
}

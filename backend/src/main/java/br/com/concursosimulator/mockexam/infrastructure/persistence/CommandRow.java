package br.com.concursosimulator.mockexam.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity @Table(name="me_command")
public class CommandRow {
    @Id UUID id;
    @Column(name="owner_kind",nullable=false,length=7) String ownerKind;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    @Column(name="operation_id",nullable=false) UUID operationId;
    @Column(name="attempt_id",nullable=false) UUID attemptId;
    @Column(name="payload_hash",nullable=false,length=64) String payloadHash;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") String outcome;
    protected CommandRow() {}
}

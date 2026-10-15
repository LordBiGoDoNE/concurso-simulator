package br.com.concursosimulator.parameterization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity @Table(name="pq_decision")
public class TemplateDecisionRow {
    @Id UUID id;
    @Column(name="template_id",nullable=false) UUID templateId;
    @Column(nullable=false) int sequence;
    @Column(nullable=false) int version;
    @Column(name="operation_id",nullable=false) UUID operationId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") String payload;
    protected TemplateDecisionRow() {}
}

package br.com.concursosimulator.parameterization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity @Table(name="pq_version")
public class TemplateVersionRow {
    @Id UUID id;
    @Column(name="template_id",nullable=false) UUID templateId;
    @Column(nullable=false) int version;
    @Column(name="base_question_id",nullable=false) UUID baseQuestionId;
    @Column(name="base_revision_id",nullable=false) UUID baseRevisionId;
    @Column(name="definition_digest",nullable=false,length=64) String definitionDigest;
    @Column(name="domain_digest",nullable=false,length=64) String domainDigest;
    @Column(name="manifest_digest",nullable=false,length=64) String manifestDigest;
    @Column(name="base_fingerprint",nullable=false,length=64) String baseFingerprint;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") String payload;
    @Column(nullable=false,length=16) String status;
    protected TemplateVersionRow() {}
}

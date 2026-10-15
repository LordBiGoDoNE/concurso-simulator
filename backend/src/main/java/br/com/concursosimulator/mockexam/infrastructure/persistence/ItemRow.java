package br.com.concursosimulator.mockexam.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity @Table(name="me_item")
public class ItemRow {
    @Id UUID id;
    @Column(name="attempt_id",nullable=false) UUID attemptId;
    @Column(nullable=false) int position;
    @Column(name="question_id",nullable=false) UUID questionId;
    @Column(name="revision_id",nullable=false) UUID revisionId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") String payload;
    protected ItemRow() {}
}

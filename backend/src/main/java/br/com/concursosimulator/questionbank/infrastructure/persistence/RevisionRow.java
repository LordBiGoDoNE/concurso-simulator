package br.com.concursosimulator.questionbank.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name="qb_revision")
public class RevisionRow {
    @Id UUID id;
    @Column(name="question_id", nullable=false) UUID questionId;
    @Column(name="revision_number", nullable=false) int number;
    @Column(nullable=false, length=64) String fingerprint;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false, columnDefinition="jsonb") String payload;
    @Column(name="discipline_id", nullable=false, length=64) String disciplineId;
    @Column(name="topic_id", nullable=false, length=16) String topicId;
    @Column(name="correct_alternative_id", nullable=false) UUID correctAlternativeId;
    @Column(nullable=false, length=16) String status;
    @ElementCollection @CollectionTable(name="qb_alternative", joinColumns=@JoinColumn(name="revision_id"))
    @OrderColumn(name="position") List<OptionValue> alternatives = new ArrayList<>();
    @ElementCollection @CollectionTable(name="qb_reference", joinColumns=@JoinColumn(name="revision_id"))
    @OrderColumn(name="position") List<ReferenceValue> references = new ArrayList<>();
    @ElementCollection @CollectionTable(name="qb_revision_role", joinColumns=@JoinColumn(name="revision_id"))
    @Column(name="role_id", nullable=false, length=64) Set<String> roles = new HashSet<>();
    protected RevisionRow() {}
}

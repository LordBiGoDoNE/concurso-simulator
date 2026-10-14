package br.com.concursosimulator.questionbank.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="qb_decision")
public class DecisionRow {
    @Id UUID id;
    @Column(name="question_id", nullable=false) UUID questionId;
    @Column(nullable=false) int sequence;
    @Column(name="revision_id", nullable=false) UUID revisionId;
    @Column(nullable=false, length=32) String action;
    @Column(nullable=false, length=64) String fingerprint;
    @Column(nullable=false, length=500) String responsible;
    @Column(name="decided_at", nullable=false) Instant at;
    @Column(nullable=false, length=300) String checklist;
    @Column(nullable=false, length=4000) String rationale;
    protected DecisionRow() {}
}

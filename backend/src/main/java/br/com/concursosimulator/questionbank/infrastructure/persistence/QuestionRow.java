package br.com.concursosimulator.questionbank.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "qb_question")
public class QuestionRow {
    @Id UUID id;
    @Column(name="origin_namespace", nullable=false, length=100) String namespace;
    @Column(name="origin_key", nullable=false, length=200) String originKey;
    @Version long version;
    @Column(name="eligible_revision_id") UUID eligibleRevisionId;
    protected QuestionRow() {}
    QuestionRow(UUID id, String namespace, String originKey) { this.id=id; this.namespace=namespace; this.originKey=originKey; }
}

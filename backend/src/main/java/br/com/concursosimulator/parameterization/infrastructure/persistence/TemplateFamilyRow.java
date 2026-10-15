package br.com.concursosimulator.parameterization.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="pq_family")
public class TemplateFamilyRow {
    @Id UUID id;
    @Column(name="base_question_id",nullable=false) UUID baseQuestionId;
    @Column(nullable=false,length=32) String kind;
    @Column(name="eligible_version") Integer eligibleVersion;
    @Version long version;
    protected TemplateFamilyRow() {}
}

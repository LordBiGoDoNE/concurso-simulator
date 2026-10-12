package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.application.port.QuestionInventory;
import br.com.concursosimulator.questionbank.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Leitura repeatable-read privada para relatório/prévia, sem carga de payload corretivo. */
public final class JpaQuestionInventory implements QuestionInventory {
    private final EntityManager entities;
    private final TransactionTemplate transactions;
    public JpaQuestionInventory(EntityManager entities, PlatformTransactionManager manager) {
        this.entities = entities; transactions = new TransactionTemplate(manager);
        transactions.setReadOnly(true); transactions.setTimeout(15);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }
    public List<Entry> read() {
        return transactions.execute(ignored -> entities.createQuery("select q from QuestionRow q order by q.id", QuestionRow.class)
                .getResultList().stream().map(q -> {
                    var revisions = entities.createQuery("select r from RevisionRow r where r.questionId=:id order by r.number", RevisionRow.class)
                            .setParameter("id", q.id).getResultList().stream().map(r -> new Revision(r.id, r.number, r.fingerprint,
                                    Question.Status.valueOf(r.status), QuestionClassification.fromIds(r.disciplineId, r.topicId, r.roles))).toList();
                    var sequence = entities.createQuery("select count(d) from DecisionRow d where d.questionId=:id", Long.class)
                            .setParameter("id", q.id).getSingleResult();
                    return new Entry(q.id, new QuestionOrigin(q.namespace, q.originKey), q.eligibleRevisionId, Math.toIntExact(sequence), revisions);
                }).toList());
    }
}

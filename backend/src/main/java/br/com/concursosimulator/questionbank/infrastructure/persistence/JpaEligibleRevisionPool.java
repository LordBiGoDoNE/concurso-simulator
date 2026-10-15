package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.application.port.EligibleRevisionPool;
import br.com.concursosimulator.questionbank.domain.QuestionContent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

@Repository
public class JpaEligibleRevisionPool implements EligibleRevisionPool {
    private final EntityManager entities;
    private final JsonMapper json=JsonMapper.builder().build();
    public JpaEligibleRevisionPool(EntityManager entities) { this.entities=entities; }
    public List<Entry> capture() {
        if (!entities.isJoinedToTransaction()) throw new IllegalStateException("Captura exige transação do consumidor");
        // Uma consulta define o ponto de elegibilidade e captura o conteúdo completo.
        var rows=entities.createQuery("select r from RevisionRow r, QuestionRow q where q.id=r.questionId and q.eligibleRevisionId=r.id and r.status='APPROVED' order by q.id",RevisionRow.class).getResultList();
        return rows.stream().map(r -> {
            var c=json.readValue(r.payload,QuestionContent.class);
            if (!c.fingerprint().equals(r.fingerprint)) throw new IllegalStateException("Fingerprint divergente");
            PublicProjectionGuard.check(c.statement(),c.alternatives(),c.references());
            return new Entry(r.questionId,r.id,r.fingerprint,c);
        }).toList();
    }
}

package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import br.com.concursosimulator.questionbank.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

/** SQL nativo restringe campos já no banco: resposta/resolução/proveniência nunca entram no resultado. */
@Repository
public class JpaEligibleQuestions implements EligibleQuestions {
    private static final String FROM = " from qb_question q join qb_revision r on r.id=q.eligible_revision_id and r.question_id=q.id where r.status='APPROVED'";
    private static final String CLASSIFICATION = ",r.discipline_id,r.topic_id,(select string_agg(rr.role_id,',' order by rr.role_id) from qb_revision_role rr where rr.revision_id=r.id)";
    private final EntityManager entities;
    private final TransactionTemplate transactions;
    private final JsonMapper json=JsonMapper.builder().build();
    public JpaEligibleQuestions(EntityManager entities, PlatformTransactionManager manager) {
        this.entities=entities; transactions=new TransactionTemplate(manager); transactions.setReadOnly(true); transactions.setTimeout(10);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }
    public Page list(Filter filter) {
        return transactions.execute(ignored -> {
            var suffix = (filter.discipline()==null?"":" and r.discipline_id=:discipline") + (filter.topic()==null?"":" and r.topic_id=:topic")
                    + (filter.role()==null?"":" and exists(select 1 from qb_revision_role rr where rr.revision_id=r.id and rr.role_id=:role)");
            long total=((Number)bind(entities.createNativeQuery("select count(*)"+FROM+suffix),filter).getSingleResult()).longValue();
            var rows=bind(entities.createNativeQuery("select q.id,r.id"+CLASSIFICATION+",r.payload->'statement'->>'text'"+FROM+suffix+" order by q.id"),filter)
                    .setFirstResult(filter.page()*filter.size()).setMaxResults(filter.size()).getResultList();
            var items=new ArrayList<Summary>();
            for(var value:rows) {
                var row=(Object[])value; var text=(String)row[5]; PublicProjectionGuard.plain(text);
                var end=text.offsetByCodePoints(0,Math.min(240,text.codePointCount(0,text.length())));
                items.add(new Summary((UUID)row[0],(UUID)row[1],classification(row),text.substring(0,end)));
            }
            return new Page(items,filter.page(),filter.size(),total);
        });
    }
    public Optional<Detail> detail(UUID id) {
        return transactions.execute(ignored -> {
            var rows=entities.createNativeQuery("select q.id,r.id"+CLASSIFICATION+",r.payload->'statement',r.payload->'alternatives',r.payload->'references'"+FROM+" and q.id=:id")
                    .setParameter("id",id).getResultList();
            if(rows.isEmpty()) return Optional.empty();
            var row=(Object[])rows.getFirst();
            var statement=json.readValue(row[5].toString(),TextContent.class);
            var options=List.of(json.readValue(row[6].toString(),QuestionContent.Alternative[].class));
            var references=List.of(json.readValue(row[7].toString(),StudyReference[].class));
            PublicProjectionGuard.check(statement,options,references);
            return Optional.of(new Detail((UUID)row[0],(UUID)row[1],classification(row),statement,options,references));
        });
    }
    private static QuestionClassification classification(Object[] row) { return QuestionClassification.fromIds((String)row[2],(String)row[3],List.of(((String)row[4]).split(","))); }
    private static Query bind(Query query,Filter filter) {
        if(filter.discipline()!=null) query.setParameter("discipline",filter.discipline().id());
        if(filter.topic()!=null) query.setParameter("topic",filter.topic().id());
        if(filter.role()!=null) query.setParameter("role",filter.role().id());
        return query;
    }
}

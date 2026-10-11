package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.util.*;
import java.util.function.Function;

/** Porta de escrita de questões: transação independente, bloqueio por agregado e retorno após commit. */
@Repository
public class JpaQuestionStore implements QuestionStore {
    private final EntityManager entities;
    private final TransactionTemplate transactions;
    private final JsonMapper json = JsonMapper.builder().build();
    public JpaQuestionStore(EntityManager entities, PlatformTransactionManager manager) {
        this.entities=entities; transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setTimeout(15);
    }
    @Override public <T> T transaction(Function<Session,T> operation) {
        return transactions.execute(status -> {
            var session=new JpaSession();
            try { var result=operation.apply(session); entities.flush(); return result; }
            finally { session.active=false; }
        });
    }
    private final class JpaSession implements Session {
        private boolean active=true;
        private final Map<UUID,Loaded> loaded=new HashMap<>();
        private void requireActive() { if (!active) throw new IllegalStateException("Unidade transacional encerrada"); }
        @Override public Optional<Question> loadForUpdate(UUID id) {
            requireActive(); if (loaded.containsKey(id)) return Optional.of(loaded.get(id).question);
            var row=entities.find(QuestionRow.class,id,LockModeType.PESSIMISTIC_WRITE);
            return Optional.ofNullable(row).map(this::load);
        }
        @Override public Optional<Question> findOriginForUpdate(QuestionOrigin origin) {
            requireActive();
            return entities.createQuery("select q from QuestionRow q where q.namespace=:namespace and q.originKey=:key",QuestionRow.class)
                    .setParameter("namespace",origin.namespace()).setParameter("key",origin.key())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().map(this::load);
        }
        private Question load(QuestionRow row) {
            if (loaded.containsKey(row.id)) return loaded.get(row.id).question;
            var revisions=entities.createQuery("select r from RevisionRow r where r.questionId=:id order by r.number",RevisionRow.class)
                    .setParameter("id",row.id).getResultList();
            var history=entities.createQuery("select d from DecisionRow d where d.questionId=:id order by d.sequence",DecisionRow.class)
                    .setParameter("id",row.id).getResultList();
            for (int i=0;i<history.size();i++) require(history.get(i).sequence==i);
            var q=Question.restore(row.id,new QuestionOrigin(row.namespace,row.originKey),
                    revisions.stream().map(JpaQuestionStore.this::readRevision).toList(),history.stream().map(JpaQuestionStore.this::readDecision).toList());
            require(Objects.equals(row.eligibleRevisionId,q.eligible().map(QuestionRevision::id).orElse(null)));
            for (var r:revisions) require(r.status.equals(q.status(r.id).name()));
            loaded.put(q.id(),new Loaded(row,q,q.revisions(),q.decisions())); return q;
        }
        @Override public void insert(Question q) {
            requireActive(); require(q.decisions().isEmpty() && q.eligible().isEmpty());
            var row=new QuestionRow(q.id(),q.origin().namespace(),q.origin().key()); entities.persist(row);
            write(q,row,0,0); loaded.put(q.id(),new Loaded(row,q,q.revisions(),q.decisions()));
        }
        @Override public void save(Question q) {
            requireActive(); var previous=loaded.get(q.id());
            if (previous==null) throw new IllegalStateException("Carregar agregado para alteração antes de salvar");
            require(q.origin().equals(new QuestionOrigin(previous.row.namespace,previous.row.originKey)));
            require(q.revisions().size()>=previous.revisions.size() && q.decisions().size()>=previous.decisions.size());
            require(q.revisions().subList(0,previous.revisions.size()).equals(previous.revisions));
            require(q.decisions().subList(0,previous.decisions.size()).equals(previous.decisions));
            write(q,previous.row,previous.revisions.size(),previous.decisions.size());
            entities.lock(previous.row,LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            loaded.put(q.id(),new Loaded(previous.row,q,q.revisions(),q.decisions()));
        }
    }
    private record Loaded(QuestionRow row,Question question,List<QuestionRevision> revisions,List<EditorialDecision> decisions) {}
    private void write(Question q,QuestionRow row,int oldRevisions,int oldDecisions) {
        for (var revision:q.revisions()) {
            if (revision.number()<=oldRevisions) {
                entities.find(RevisionRow.class,revision.id()).status=q.status(revision.id()).name();
            } else {
                var c=revision.content(); var r=new RevisionRow();
                r.id=revision.id(); r.questionId=q.id(); r.number=revision.number(); r.fingerprint=revision.fingerprint();
                r.payload=json.writeValueAsString(c); r.disciplineId=c.classification().discipline().id(); r.topicId=c.classification().topic().id();
                r.correctAlternativeId=c.correctAlternativeId(); r.status=q.status(revision.id()).name();
                r.alternatives.addAll(c.alternatives().stream().map(a -> new OptionValue(a.id())).toList());
                r.roles.addAll(c.classification().roles().stream().map(TargetRole::id).toList());
                r.references.addAll(c.references().stream().map(ref -> new ReferenceValue(ref.name(),ref.url().toString(),
                        ref.legalBasis()==null?null:ref.legalBasis().provision(),ref.legalBasis()==null?null:ref.legalBasis().checkedOn())).toList());
                entities.persist(r);
            }
        }
        for (int i=oldDecisions;i<q.decisions().size();i++) {
            var d=q.decisions().get(i); var r=new DecisionRow(); r.id=UUID.randomUUID(); r.questionId=q.id(); r.sequence=i;
            r.revisionId=d.revisionId(); r.action=d.action().name(); r.fingerprint=d.fingerprint(); r.responsible=d.responsible();
            r.at=d.at(); r.checklist=String.join(",",d.checklist().stream().map(Enum::name).sorted().toList()); r.rationale=d.rationale();
            entities.persist(r);
        }
        row.eligibleRevisionId=q.eligible().map(QuestionRevision::id).orElse(null);
    }
    private QuestionRevision readRevision(RevisionRow r) {
        var c=json.readValue(r.payload,QuestionContent.class);
        require(c.fingerprint().equals(r.fingerprint)); require(c.classification().discipline().id().equals(r.disciplineId));
        require(c.classification().topic().id().equals(r.topicId)); require(c.correctAlternativeId().equals(r.correctAlternativeId));
        require(c.alternatives().stream().map(QuestionContent.Alternative::id).toList().equals(r.alternatives.stream().map(a->a.optionId).toList()));
        require(c.classification().roles().stream().map(TargetRole::id).collect(java.util.stream.Collectors.toSet()).equals(r.roles));
        require(c.references().equals(r.references.stream().map(ref->new StudyReference(ref.name,URI.create(ref.url),
                ref.provision==null?null:new StudyReference.LegalBasis(ref.provision,ref.checkedOn))).toList()));
        return new QuestionRevision(r.id,r.number,c);
    }
    private EditorialDecision readDecision(DecisionRow r) {
        var checks=new HashSet<EditorialDecision.Check>();
        if (!r.checklist.isEmpty()) for (var value:r.checklist.split(",")) checks.add(EditorialDecision.Check.valueOf(value));
        return new EditorialDecision(EditorialDecision.Action.valueOf(r.action),r.questionId,r.revisionId,r.fingerprint,r.responsible,r.at,checks,r.rationale);
    }
    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("Conteúdo/histórico/metadados de persistência inconsistentes");
    }
}

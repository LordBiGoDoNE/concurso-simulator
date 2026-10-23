package br.com.concursosimulator.parameterization.infrastructure.persistence;

import br.com.concursosimulator.parameterization.application.TemplateStore;
import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.QuestionContent;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.function.Function;

@Repository
public class JpaTemplateStore implements TemplateStore {
    private final EntityManager entities;
    private final TransactionTemplate transactions;
    static final JsonMapper JSON=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES).build();
    public JpaTemplateStore(EntityManager entities,PlatformTransactionManager manager) {
        this.entities=entities;transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);transactions.setTimeout(15);
    }
    public <T>T transaction(Function<Session,T> action) {
        try {
            return transactions.execute(status -> {
                var session=new JpaSession();
                try {var result=action.apply(session);entities.flush();return result;}finally{session.active=false;}
            });
        }catch(RuntimeException failure) {
            // Fora de execute: rollback concluído inclusive se falhou flush/commit.
            for(Throwable cause=failure;cause!=null;cause=cause.getCause()) {
                if(cause instanceof ConstraintViolationException c && Set.of("pq_family_base_key","pq_family_pkey").contains(c.getConstraintName()))throw new ConcurrentRegistration(failure);
                if(cause instanceof java.sql.SQLException sql && "40001".equals(sql.getSQLState()))throw new Stale();
            }
            throw failure;
        }
    }
    private record Loaded(TemplateFamilyRow row,TemplateFamily family,List<TemplateVersion> versions,List<TemplateDecision> decisions) {}
    private final class JpaSession implements Session {
        boolean active=true;final Map<UUID,Loaded> loaded=new HashMap<>();
        void active(){if(!active)throw new IllegalStateException("Transação encerrada");}
        public Optional<TemplateFamily> loadForUpdate(UUID id) {
            active();if(loaded.containsKey(id))return Optional.of(loaded.get(id).family());
            return Optional.ofNullable(entities.find(TemplateFamilyRow.class,id,LockModeType.PESSIMISTIC_WRITE)).map(this::read);
        }
        public Optional<TemplateFamily> findBaseForUpdate(UUID questionId) {
            active();return entities.createQuery("select f from TemplateFamilyRow f where f.baseQuestionId=:id",TemplateFamilyRow.class)
                    .setParameter("id",questionId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().map(this::read);
        }
        public Optional<BaseRevision> base(UUID questionId,UUID revisionId) {
            active();List<?> rows=entities.createNativeQuery("select r.question_id,r.id,r.fingerprint,r.payload::text,(q.eligible_revision_id=r.id and r.status='APPROVED') from qb_revision r join qb_question q on q.id=r.question_id where r.question_id=:q and r.id=:r",Object[].class)
                    .setParameter("q",questionId).setParameter("r",revisionId).getResultList();
            return rows.stream().map(Object[].class::cast).findFirst().map(r -> new BaseRevision((UUID)r[0],(UUID)r[1],(String)r[2],JSON.readValue((String)r[3],QuestionContent.class),Boolean.TRUE.equals(r[4])));
        }
        private TemplateFamily read(TemplateFamilyRow row) {
            if(loaded.containsKey(row.id))return loaded.get(row.id).family();
            var versions=entities.createQuery("select v from TemplateVersionRow v where v.templateId=:id order by v.version",TemplateVersionRow.class).setParameter("id",row.id).getResultList();
            var decisions=entities.createQuery("select d from TemplateDecisionRow d where d.templateId=:id order by d.sequence",TemplateDecisionRow.class).setParameter("id",row.id).getResultList();
            var history=new ArrayList<TemplateDecision>();
            for(int i=0;i<decisions.size();i++) {
                var r=decisions.get(i);var d=JSON.readValue(r.payload,TemplateDecision.class);
                require(r.sequence==i && r.templateId.equals(d.templateId()) && r.version==d.version() && r.operationId.equals(d.operationId()));history.add(d);
            }
            var family=TemplateFamily.restore(versions.stream().map(JpaTemplateStore::readVersion).toList(),history);
            require(row.id.equals(family.id()) && row.baseQuestionId.equals(family.baseQuestionId()) && row.kind.equals(family.versions().getFirst().definition().family().name()) && Objects.equals(row.eligibleVersion,family.eligibleVersion()));
            for(var v:versions)require(v.status.equals(family.status(v.version).name()));
            loaded.put(row.id,new Loaded(row,family,family.versions(),family.decisions()));return family;
        }
        public void insert(TemplateFamily family) {
            active();require(family.versions().size()==1 && family.decisions().isEmpty() && family.eligible().isEmpty());
            var row=new TemplateFamilyRow();row.id=family.id();row.baseQuestionId=family.baseQuestionId();row.kind=family.versions().getFirst().definition().family().name();
            entities.persist(row);entities.flush();write(family,row,0,0);loaded.put(row.id,new Loaded(row,family,family.versions(),family.decisions()));
        }
        public void save(TemplateFamily family) {
            active();var previous=loaded.get(family.id());require(previous!=null);
            require(previous.row().baseQuestionId.equals(family.baseQuestionId()) && family.versions().size()>=previous.versions().size() && family.decisions().size()>=previous.decisions().size()
                    && family.versions().subList(0,previous.versions().size()).equals(previous.versions()) && family.decisions().subList(0,previous.decisions().size()).equals(previous.decisions()));
            write(family,previous.row(),previous.versions().size(),previous.decisions().size());entities.lock(previous.row(),LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            loaded.put(family.id(),new Loaded(previous.row(),family,family.versions(),family.decisions()));
        }
    }
    private void write(TemplateFamily family,TemplateFamilyRow row,int oldVersions,int oldDecisions) {
        for(var v:family.versions()) {
            var d=v.definition();
            if(d.version()<=oldVersions) {
                var r=entities.createQuery("select v from TemplateVersionRow v where v.templateId=:id and v.version=:version",TemplateVersionRow.class).setParameter("id",family.id()).setParameter("version",d.version()).getSingleResult();
                r.status=family.status(d.version()).name();
            }else{
                var r=new TemplateVersionRow();r.id=UUID.randomUUID();r.templateId=d.id();r.version=d.version();r.baseQuestionId=d.baseQuestionId();r.baseRevisionId=d.baseRevisionId();
                r.definitionDigest=d.digest();r.domainDigest=d.family().domainDigest();r.manifestDigest=v.manifest().digest();r.baseFingerprint=d.baseFingerprint();r.payload=JSON.writeValueAsString(v);r.status=family.status(d.version()).name();entities.persist(r);
            }
        }
        entities.flush();
        for(int i=oldDecisions;i<family.decisions().size();i++) {
            var d=family.decisions().get(i);var r=new TemplateDecisionRow();r.id=UUID.randomUUID();r.templateId=d.templateId();r.sequence=i;r.version=d.version();r.operationId=d.operationId();r.payload=JSON.writeValueAsString(d);entities.persist(r);
        }
        row.eligibleVersion=family.eligibleVersion();
    }
    static TemplateVersion readVersion(TemplateVersionRow r) {
        var v=JSON.readValue(r.payload,TemplateVersion.class);var d=v.definition();
        require(r.templateId.equals(d.id()) && r.version==d.version() && r.baseQuestionId.equals(d.baseQuestionId()) && r.baseRevisionId.equals(d.baseRevisionId())
                && r.definitionDigest.equals(d.digest()) && r.domainDigest.equals(d.family().domainDigest()) && r.manifestDigest.equals(v.manifest().digest()) && r.baseFingerprint.equals(d.baseFingerprint()));
        return v;
    }
    private static void require(boolean valid){if(!valid)throw new IllegalStateException("Registro de template inconsistente");}
}

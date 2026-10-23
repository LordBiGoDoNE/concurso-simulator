package br.com.concursosimulator.parameterization.infrastructure.persistence;

import br.com.concursosimulator.parameterization.application.EligibleTemplates;
import br.com.concursosimulator.parameterization.domain.TemplateVersion;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class JpaEligibleTemplates implements EligibleTemplates {
    private final EntityManager entities;
    public JpaEligibleTemplates(EntityManager entities){this.entities=entities;}
    public List<TemplateVersion> capture() {
        if(!entities.isJoinedToTransaction())throw new IllegalStateException("Captura exige transação do consumidor");
        return entities.createQuery("select v from TemplateVersionRow v,TemplateFamilyRow f,QuestionRow q,RevisionRow r where v.templateId=f.id and v.version=f.eligibleVersion and v.status='APPROVED' and f.baseQuestionId=q.id and q.eligibleRevisionId=r.id and r.questionId=q.id and r.id=v.baseRevisionId and r.fingerprint=v.baseFingerprint and r.status='APPROVED' order by f.id",TemplateVersionRow.class)
                .getResultList().stream().map(JpaTemplateStore::readVersion).toList();
    }
}

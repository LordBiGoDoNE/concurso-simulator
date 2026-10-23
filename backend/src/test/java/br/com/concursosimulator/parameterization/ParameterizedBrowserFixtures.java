package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.TemplateUseCases;
import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.context.ConfigurableApplicationContext;
import java.time.Instant;
import java.util.*;

/** Fixture sintética explícita de HTTP/browser, nunca JAR, seed ou decisão editorial real. */
public final class ParameterizedBrowserFixtures {
    private ParameterizedBrowserFixtures() {}
    public static List<TemplateVersion> seed(ConfigurableApplicationContext context) {
        var versions=new ArrayList<TemplateVersion>();var questions=context.getBean(QuestionStore.class);
        var templates=context.getBean(br.com.concursosimulator.parameterization.application.TemplateStore.class);
        for(var family:Family.values()) {
            var d=ParameterFixtures.definition(family);var old=br.com.concursosimulator.questionbank.QuestionFixtures.content("PRIVATE_PARAMETERIZED_FIXTURE "+family);
            var content=new QuestionContent(old.statement(),old.alternatives(),old.correctAlternativeId(),old.resolution(),d.classification(),d.references(),d.provenance());
            insertApproved(questions,d.baseQuestionId(),d.baseRevisionId(),content);
            var definition=new TemplateDefinition(d.id(),1,family,d.generatorVersion(),d.baseQuestionId(),d.baseRevisionId(),content.fingerprint(),d.classification(),d.references(),d.provenance());
            var cases=new TemplateUseCases(templates);var v=cases.register(definition,0).version();
            cases.decide(ParameterFixtures.decision(v,TemplateDecision.Action.SUBMIT),null,0);
            cases.decide(ParameterFixtures.decision(v,TemplateDecision.Action.APPROVE),null,1);versions.add(v);
        }
        var d=ParameterFixtures.definition(Family.DIRECT_PROPORTION);var old=br.com.concursosimulator.questionbank.QuestionFixtures.content("Questão fixa sintética de matemática");
        insertApproved(questions,UUID.randomUUID(),UUID.randomUUID(),new QuestionContent(old.statement(),old.alternatives(),old.correctAlternativeId(),old.resolution(),d.classification(),old.references(),old.provenance()));
        return List.copyOf(versions);
    }
    private static void insertApproved(QuestionStore store,UUID id,UUID revision,QuestionContent content) {
        var q=Question.draft(id,new QuestionOrigin("parameterized-fixture-only",id.toString()),revision,content);
        store.transaction(s->{s.insert(q);return null;});
        for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE))store.transaction(s->{
            var loaded=s.loadForUpdate(id).orElseThrow();loaded.decide(new EditorialDecision(action,id,revision,content.fingerprint(),
                    "Fixture sem validade editorial",Instant.now(),Set.of(EditorialDecision.Check.values()),"Somente PostgreSQL isolado de testes"));s.save(loaded);return null;
        });
    }
}

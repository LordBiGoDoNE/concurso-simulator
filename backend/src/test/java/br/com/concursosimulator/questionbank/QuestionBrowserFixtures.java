package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.QuestionStore;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.*;
import java.time.Instant;

/** Somente harness de navegador em src/test, nunca no JAR/seed/startup de produção. */
public final class QuestionBrowserFixtures {
    public static void seed(ConfigurableApplicationContext context) {
        var store=context.getBean(QuestionStore.class);var candidates=new ArrayList<ApplyQuestionCandidatesUseCase.Candidate>();
        for(int i=1;i<=24;i++) {
            var fixture=QuestionFixtures.content("Questão sintética "+i+": [destaque: πr²] e (1)/(2); (x)_(1) + (y)_(2); 2^(3); H_(2)O; _abc; x < y e y > z, sem correção.");
            var classification=i==22?new QuestionClassification(Discipline.MATHEMATICS_LOGIC,Topic.M01,Set.of(TargetRole.SCHOOL_SECRETARY)):fixture.classification();
            var content=new QuestionContent(fixture.statement(),fixture.alternatives(),fixture.correctAlternativeId(),
                    new TextContent("PRIVATE_BROWSER_RESOLUTION_SENTINEL — não transmitir"),classification,fixture.references(),fixture.provenance());
            candidates.add(new ApplyQuestionCandidatesUseCase.Candidate(new QuestionOrigin("fixture-browser-only","item-"+i),content,null));
        }
        var results=new ApplyQuestionCandidatesUseCase(store).execute(candidates);
        for(var result:results) {
            if(result.origin().key().equals("item-23")) continue; // rascunho privado
            for(var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE)) decide(store,result,action);
            if(result.origin().key().equals("item-24")) decide(store,result,EditorialDecision.Action.WITHDRAW);
        }
    }
    private static void decide(QuestionStore store,ApplyQuestionCandidatesUseCase.Result result,EditorialDecision.Action action) {
        var q=store.transaction(s -> s.loadForUpdate(result.questionId()).orElseThrow());
        new DecideQuestionUseCase(store).execute(new EditorialDecision(action,q.id(),result.revisionId(),q.revision(result.revisionId()).fingerprint(),
                "Fixture de navegador, sem validade editorial",Instant.now(),Set.of(EditorialDecision.Check.values()),"Banco Testcontainers exclusivo, não aprovação do acervo"),
                q.eligible().map(QuestionRevision::id).orElse(null),q.editorialSequence());
    }
}

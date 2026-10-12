package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.BrowseQuestionsUseCase;
import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;
import java.util.*;

@RestController
@RequestMapping("/api/v1/question-bank")
public class QuestionController {
    private final BrowseQuestionsUseCase browse;
    public QuestionController(BrowseQuestionsUseCase browse) { this.browse=browse; }
    @GetMapping("/catalog") public ResponseEntity<QuestionResponses.Catalog> catalog(@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        return ok(new QuestionResponses.Catalog(TargetRole.CONTEST_ID,
                Arrays.stream(Discipline.values()).map(d -> new QuestionResponses.Named(d.id(),d.title())).toList(),
                Arrays.stream(Topic.values()).map(t -> new QuestionResponses.TopicItem(t.id(),t.discipline().id(),t.title())).toList(),
                Arrays.stream(TargetRole.values()).map(r -> new QuestionResponses.Named(r.id(),r.title())).toList()));
    }
    @GetMapping("/questions") public ResponseEntity<QuestionResponses.Page> list(@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of("disciplineId","topicId","roleId","page","size"));
        var filter=new EligibleQuestions.Filter(params.containsKey("disciplineId")?Discipline.fromId(params.getFirst("disciplineId")):null,
                params.containsKey("topicId")?Topic.fromId(params.getFirst("topicId")):null,
                params.containsKey("roleId")?TargetRole.fromId(params.getFirst("roleId")):null,
                number(params.getFirst("page"),0),number(params.getFirst("size"),20));
        return ok(QuestionResponses.Page.from(browse.list(filter)));
    }
    @GetMapping("/questions/{questionId}") public ResponseEntity<QuestionResponses.Detail> detail(@PathVariable String questionId,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        if(!questionId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw new IllegalArgumentException("UUID inválido");
        return ok(QuestionResponses.Detail.from(browse.detail(UUID.fromString(questionId))));
    }
    private static int number(String value,int defaultValue) { if(value==null) return defaultValue; if(!value.matches("[0-9]+")) throw new IllegalArgumentException("Número inválido"); return Integer.parseInt(value); }
    private static void parameters(MultiValueMap<String,String> params,Set<String> allowed) {
        if(!allowed.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size()!=1 || v.getFirst().isBlank())) throw new IllegalArgumentException("Parâmetro inválido/repetido");
    }
    private static <T> ResponseEntity<T> ok(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
}

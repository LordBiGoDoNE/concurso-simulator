package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.BrowseQuestionsUseCase;
import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.shared.web.RequestValues;
import java.util.*;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/question-bank")
public class QuestionController {
    private final BrowseQuestionsUseCase browse;

    public QuestionController(BrowseQuestionsUseCase browse) {
        this.browse = browse;
    }

    @GetMapping("/catalog")
    public ResponseEntity<QuestionResponses.Catalog> catalog(@RequestParam MultiValueMap<String, String> params) {
        parameters(params, Set.of());
        return ok(new QuestionResponses.Catalog(
                TargetRole.CONTEST_ID,
                Arrays.stream(Discipline.values())
                        .map(d -> new QuestionResponses.Named(d.id(), d.title()))
                        .toList(),
                Arrays.stream(Topic.values())
                        .map(t -> new QuestionResponses.TopicItem(
                                t.id(), t.discipline().id(), t.title()))
                        .toList(),
                Arrays.stream(TargetRole.values())
                        .map(r -> new QuestionResponses.Named(r.id(), r.title()))
                        .toList()));
    }

    @GetMapping("/questions")
    public ResponseEntity<QuestionResponses.Page> list(@RequestParam MultiValueMap<String, String> params) {
        parameters(params, Set.of("disciplineId", "topicId", "roleId", "page", "size"));
        var page = number(params.getFirst("page"), 0);
        var size = number(params.getFirst("size"), 20);
        var filter = RequestValues.parse(() -> new EligibleQuestions.Filter(
                params.containsKey("disciplineId") ? Discipline.fromId(params.getFirst("disciplineId")) : null,
                params.containsKey("topicId") ? Topic.fromId(params.getFirst("topicId")) : null,
                params.containsKey("roleId") ? TargetRole.fromId(params.getFirst("roleId")) : null,
                page,
                size));
        return ok(QuestionResponses.Page.from(browse.list(filter)));
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponses.Detail> detail(
            @PathVariable String questionId, @RequestParam MultiValueMap<String, String> params) {
        parameters(params, Set.of());
        RequestValues.require(
                questionId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
        return ok(QuestionResponses.Detail.from(browse.detail(UUID.fromString(questionId))));
    }

    private static int number(String value, int defaultValue) {
        if (value == null) return defaultValue;
        RequestValues.require(value.matches("[0-9]+"));
        return RequestValues.parse(() -> Integer.parseInt(value));
    }

    private static void parameters(MultiValueMap<String, String> params, Set<String> allowed) {
        RequestValues.require(allowed.containsAll(params.keySet())
                && params.values().stream()
                        .allMatch(v -> v.size() == 1 && !v.getFirst().isBlank()));
    }

    private static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}

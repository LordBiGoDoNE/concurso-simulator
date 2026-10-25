package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static br.com.concursosimulator.mockexam.web.AttemptContracts.*;

@RestController
public final class LessonPracticeController {
    public record Availability(String topicId,String roleId,int available,int maximum) {}
    public record Start(String commandId,String roleId,String topicId) {}
    public record HistoryItem(UUID id,String roleId,String topicId,Instant createdAt,Instant finishedAt,int total,java.math.BigDecimal percentage) {}
    public record History(List<HistoryItem> items,int page,int size) {}
    private final AttemptUseCases cases;
    public LessonPracticeController(@Qualifier("lessonPracticeUseCases") AttemptUseCases cases) { this.cases=cases; }
    @PostMapping("/api/v1/lesson-practices") public ResponseEntity<View> create(@CurrentActor AttemptOwner actor,@RequestBody Start body,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of());
        var outcome=cases.createPractice(actor,uuid(body.commandId()),TargetRole.fromId(body.roleId()),Topic.fromId(body.topicId()));
        return ResponseEntity.status(outcome.replayed()?200:201).cacheControl(org.springframework.http.CacheControl.noStore()).body(View.from(outcome.attempt()));
    }
    @GetMapping("/api/v1/lesson-practices/availability") public ResponseEntity<Availability> availability(@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of("roleId","topicId"));
        var value=cases.practiceAvailability(TargetRole.fromId(params.getFirst("roleId")),Topic.fromId(params.getFirst("topicId")));
        return AttemptController.ok(new Availability(value.topicId(),value.roleId(),value.available(),value.maximum()));
    }
    @GetMapping("/api/v1/lesson-practices/{id}") public ResponseEntity<View> read(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of());return AttemptController.ok(View.from(cases.read(actor,uuid(id))));
    }
    @PutMapping("/api/v1/lesson-practices/{id}/answers/{item}") public ResponseEntity<View> answer(@CurrentActor AttemptOwner actor,@PathVariable String id,@PathVariable String item,@RequestBody AnswerRequest body,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of());return AttemptController.ok(View.from(cases.answer(actor,uuid(id),uuid(body.commandId()),version(body.expectedVersion()),uuid(item),body.alternativeId()==null?null:uuid(body.alternativeId())).attempt()));
    }
    @PostMapping("/api/v1/lesson-practices/{id}/finish") public ResponseEntity<Finished> finish(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestBody FinishRequest body,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of());var result=cases.finish(actor,uuid(id),uuid(body.commandId()),version(body.expectedVersion()));
        return AttemptController.ok(new Finished(View.from(result.attempt()),Result.from(result.result())));
    }
    @GetMapping("/api/v1/lesson-practices/{id}/result") public ResponseEntity<Result> result(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of());return AttemptController.ok(Result.from(cases.result(actor,uuid(id))));
    }
    @GetMapping("/api/v1/me/lesson-practices") public ResponseEntity<History> history(@CurrentActor AttemptOwner actor,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of("page","size","finished"));
        var page=number(params.getFirst("page"),0);var size=number(params.getFirst("size"),20);var finished=params.getFirst("finished");
        if (finished!=null && !Set.of("true","false").contains(finished)) throw invalid();
        var entries=cases.history(actor,new AttemptStore.HistoryQuery(page,size,finished==null?null:Boolean.valueOf(finished),QuestionPurpose.PRATICA_AULA));
        return AttemptController.ok(new History(entries.stream().map(e -> new HistoryItem(e.id(),TargetRole.valueOf(e.role()).id(),e.topicId(),e.createdAt(),e.finishedAt(),e.total(),e.percentage())).toList(),page,size));
    }
    private static int number(String value,int fallback) {
        if (value==null) return fallback;if (!value.matches("[0-9]+")) throw invalid();return Integer.parseInt(value);
    }
}

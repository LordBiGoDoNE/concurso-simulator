package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.application.AttemptUseCases;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;
import java.util.Set;
import static br.com.concursosimulator.mockexam.web.AttemptContracts.*;

@RestController
@RequestMapping("/api/v1/attempts")
public class AttemptController {
    private final AttemptUseCases cases;
    public AttemptController(AttemptUseCases cases) { this.cases=cases; }
    @PostMapping public ResponseEntity<View> create(@CurrentActor AttemptOwner actor,@RequestBody CreateRequest body,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        var outcome=cases.create(actor,uuid(body.commandId()),body.composition());
        return ResponseEntity.status(outcome.replayed()?200:201).cacheControl(CacheControl.noStore()).body(View.from(outcome.attempt()));
    }
    @GetMapping("/{id}") public ResponseEntity<View> read(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        return ok(View.from(cases.read(actor,uuid(id))));
    }
    @PutMapping("/{id}/answers/{item}") public ResponseEntity<View> answer(@CurrentActor AttemptOwner actor,@PathVariable String id,@PathVariable String item,@RequestBody AnswerRequest body,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        return ok(View.from(cases.answer(actor,uuid(id),uuid(body.commandId()),version(body.expectedVersion()),uuid(item),
                body.alternativeId()==null?null:uuid(body.alternativeId())).attempt()));
    }
    @PostMapping("/{id}/finish") public ResponseEntity<Finished> finish(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestBody FinishRequest body,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        var result=cases.finish(actor,uuid(id),uuid(body.commandId()),version(body.expectedVersion()));
        return ok(new Finished(View.from(result.attempt()),Result.from(result.result())));
    }
    @GetMapping("/{id}/result") public ResponseEntity<Result> result(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of()); return ok(Result.from(cases.result(actor,uuid(id))));
    }
    static <T> ResponseEntity<T> ok(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    static void parameters(MultiValueMap<String,String> params,Set<String> allowed) {
        if (!allowed.containsAll(params.keySet()) || params.values().stream().anyMatch(v -> v.size()!=1 || v.getFirst().isBlank())) throw invalid();
    }
}

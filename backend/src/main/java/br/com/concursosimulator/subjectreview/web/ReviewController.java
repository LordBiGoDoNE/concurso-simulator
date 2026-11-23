package br.com.concursosimulator.subjectreview.web;

import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.mockexam.web.CurrentActor;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import br.com.concursosimulator.shared.web.RequestValues;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class ReviewController {
    private final ReviewUseCases cases;
    public ReviewController(ReviewUseCases cases) { this.cases=cases; }
    @GetMapping("/api/v1/me/subject-review")
    public ResponseEntity<ReviewContracts.Overview> overview(@CurrentActor AttemptOwner actor,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of("roleId"));
        RequestValues.require(params.containsKey("roleId"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ReviewContracts.Overview.from(cases.overview(actor,RequestValues.parse(() -> TargetRole.fromId(params.getFirst("roleId"))))));
    }
    @GetMapping("/api/v1/attempts/{id}/subject-review")
    public ResponseEntity<ReviewContracts.Individual> individual(@CurrentActor AttemptOwner actor,@PathVariable String id,@RequestParam MultiValueMap<String,String> params) {
        parameters(params,Set.of());
        RequestValues.require(id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ReviewContracts.Individual.from(cases.individual(actor,UUID.fromString(id))));
    }
    private static void parameters(MultiValueMap<String,String> params,Set<String> allowed) {
        RequestValues.require(allowed.containsAll(params.keySet()) && params.values().stream().allMatch(v->v.size()==1 && !v.getFirst().isBlank()));
    }
}

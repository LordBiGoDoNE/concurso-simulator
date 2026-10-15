package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import static br.com.concursosimulator.mockexam.web.AttemptContracts.*;

@RestController
public class AttemptHistoryController {
    private final AttemptUseCases cases;
    public AttemptHistoryController(AttemptUseCases cases) { this.cases=cases; }
    @GetMapping("/api/v1/me/attempts") public ResponseEntity<History> history(@CurrentActor AttemptOwner actor,@RequestParam MultiValueMap<String,String> params) {
        AttemptController.parameters(params,Set.of("page","size","finished"));
        var page=number(params.getFirst("page"),0); var size=number(params.getFirst("size"),20);
        var finished=params.getFirst("finished");
        if (finished!=null && !Set.of("true","false").contains(finished)) throw invalid();
        var items=cases.history(actor,new AttemptStore.HistoryQuery(page,size,finished==null?null:Boolean.valueOf(finished)));
        return AttemptController.ok(new History(items.stream().map(e -> new HistoryItem(e.id(),TargetRole.valueOf(e.role()).id(),e.createdAt(),e.finishedAt(),e.total(),e.percentage())).toList(),page,size));
    }
    private static int number(String value,int fallback) {
        if (value==null) return fallback; if (!value.matches("[0-9]+")) throw invalid(); return Integer.parseInt(value);
    }
}

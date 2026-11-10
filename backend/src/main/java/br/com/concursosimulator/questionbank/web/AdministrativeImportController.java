package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import br.com.concursosimulator.questionbank.application.port.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/question-imports")
public class AdministrativeImportController {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(AdministrativeImportController.class);
    private final QuestionImportReader reader;private final PreviewQuestionCandidatesUseCase preview;
    private final ApplyAdministrativeQuestionsUseCase apply;private final QuestionImportConfirmations tokens;private final QuestionImportTarget target;
    public AdministrativeImportController(QuestionImportReader reader,PreviewQuestionCandidatesUseCase preview,
            ApplyAdministrativeQuestionsUseCase apply,QuestionImportConfirmations tokens,QuestionImportTarget target){
        this.reader=reader;this.preview=preview;this.apply=apply;this.tokens=tokens;this.target=target;
    }
    public record PreviewRequest(@JsonProperty("package") QuestionImportReader.Document document,List<String> keys) {}
    public record ApplyRequest(@JsonProperty("package") QuestionImportReader.Document document,List<String> keys,String confirmationToken) {}
    public record ItemPreview(br.com.concursosimulator.questionbank.domain.QuestionOrigin origin,String fingerprint,
            UUID expectedLatestRevision,PreviewQuestionCandidatesUseCase.Change change,UUID questionId,UUID latestRevisionId) {}
    public record PreviewResponse(QuestionPurpose purpose,String destinationId,String packageDigest,String selectionDigest,
            List<ItemPreview> items,List<List<String>> possibleDuplicates,boolean confirmable,String confirmationToken,Instant expiresAt) {}
    public record ApplyResponse(QuestionPurpose purpose,String destinationId,ImportReceipt receipt,boolean replayed) {}
    @PostMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(@RequestBody PreviewRequest request,@AuthenticationPrincipal UserPrincipal actor){
        var pack=reader.read(request.document(),request.keys());var results=preview.execute(pack.candidates());
        var items=new ArrayList<ItemPreview>();
        for(int i=0;i<results.size();i++){
            var r=results.get(i);var c=pack.candidates().get(i);
            items.add(new ItemPreview(r.origin(),c.content().fingerprint(),c.expectedLatestRevision(),r.change(),r.questionId(),r.latestRevisionId()));
        }
        boolean confirmable=results.stream().noneMatch(r->r.change()==PreviewQuestionCandidatesUseCase.Change.STALE);
        var issued=confirmable?tokens.issue(actor.id(),target.id(),pack):null;
        LOG.info("Question import preview actor={} destination={} purpose={} packageDigest={} selectionDigest={} keys={} confirmable={}",actor.id(),target.id(),pack.purpose(),pack.digest(),pack.selectionDigest(),request.keys(),confirmable);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new PreviewResponse(pack.purpose(),target.id(),pack.digest(),pack.selectionDigest(),List.copyOf(items),pack.duplicates(),confirmable,issued==null?null:issued.token(),issued==null?null:issued.expiresAt()));
    }
    @PostMapping("/apply")
    public ResponseEntity<ApplyResponse> apply(@RequestBody ApplyRequest request,@AuthenticationPrincipal UserPrincipal actor){
        var pack=reader.read(request.document(),request.keys());
        var outcome=apply.execute(pack,tokens.verify(request.confirmationToken()),actor.id(),target.id());
        LOG.info("Question import committed actor={} destination={} packageDigest={} selectionDigest={} nonce={} replayed={}",actor.id(),target.id(),pack.digest(),pack.selectionDigest(),outcome.receipt().confirmation().nonce(),outcome.replayed());
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new ApplyResponse(pack.purpose(),target.id(),outcome.receipt(),outcome.replayed()));
    }
}

package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.identity.application.UserPrincipal;
import br.com.concursosimulator.questionbank.application.*;
import static br.com.concursosimulator.questionbank.web.AdministrativeContracts.*;
import br.com.concursosimulator.questionbank.application.port.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/v1/admin/question-imports",consumes=org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
public class AdministrativeImportController {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(AdministrativeImportController.class);
    private final QuestionImportReader reader;private final PreviewQuestionCandidatesUseCase preview;
    private final ApplyAdministrativeQuestionsUseCase apply;private final QuestionImportConfirmations tokens;private final QuestionImportTarget target;
    public AdministrativeImportController(QuestionImportReader reader,PreviewQuestionCandidatesUseCase preview,
            ApplyAdministrativeQuestionsUseCase apply,QuestionImportConfirmations tokens,QuestionImportTarget target){
        this.reader=reader;this.preview=preview;this.apply=apply;this.tokens=tokens;this.target=target;
    }
    public record PreviewRequest(@JsonProperty("package") PackageDocument document,List<String> keys) {}
    public record ApplyRequest(@JsonProperty("package") PackageDocument document,List<String> keys,String confirmationToken) {}
    public record ItemPreview(Origin origin,String fingerprint,UUID expectedLatestRevision,String change,UUID questionId,UUID latestRevisionId) {}
    public record PreviewResponse(Purpose purpose,String destinationId,String packageDigest,String selectionDigest,
            List<ItemPreview> items,List<List<String>> possibleDuplicates,boolean confirmable,String confirmationToken,Instant expiresAt) {}
    public record ApplyResponse(Purpose purpose,String destinationId,ImportReceiptView receipt,boolean replayed) {}
    /** O pacote já foi validado pelo {@link ImportRequestBodyAdvice}; recusa aqui ainda é pacote inválido, nunca 500. */
    static QuestionImportReader.Document application(PackageDocument document) {
        try { return AdministrativeContracts.document(document); }
        catch (IllegalArgumentException | NullPointerException refused) { throw new QuestionImportReader.InvalidPackage(); }
    }
    @PostMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(@RequestBody PreviewRequest request,@AuthenticationPrincipal UserPrincipal actor){
        var pack=reader.read(application(request.document()),request.keys());var results=preview.execute(pack.candidates());
        var items=new ArrayList<ItemPreview>();
        for(int i=0;i<results.size();i++){
            var r=results.get(i);var c=pack.candidates().get(i);
            items.add(new ItemPreview(AdministrativeContracts.origin(r.origin()),c.content().fingerprint(),c.expectedLatestRevision(),AdministrativeContracts.name(r.change()),r.questionId(),r.latestRevisionId()));
        }
        boolean confirmable=results.stream().noneMatch(r->r.change()==PreviewQuestionCandidatesUseCase.Change.STALE);
        var issued=confirmable?tokens.issue(actor.id(),target.id(),pack):null;
        LOG.info("Question import preview actor={} destination={} purpose={} packageDigest={} selectionDigest={} keys={} confirmable={}",actor.id(),target.id(),pack.purpose(),pack.digest(),pack.selectionDigest(),request.keys(),confirmable);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new PreviewResponse(AdministrativeContracts.map(pack.purpose(),Purpose.class),target.id(),pack.digest(),pack.selectionDigest(),List.copyOf(items),pack.duplicates(),confirmable,issued==null?null:issued.token(),issued==null?null:issued.expiresAt()));
    }
    @PostMapping("/apply")
    public ResponseEntity<ApplyResponse> apply(@RequestBody ApplyRequest request,@AuthenticationPrincipal UserPrincipal actor){
        var pack=reader.read(application(request.document()),request.keys());
        var outcome=apply.execute(pack,tokens.verify(request.confirmationToken()),actor.id(),target.id());
        LOG.info("Question import committed actor={} destination={} packageDigest={} selectionDigest={} nonce={} replayed={}",actor.id(),target.id(),pack.digest(),pack.selectionDigest(),outcome.receipt().confirmation().nonce(),outcome.replayed());
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new ApplyResponse(AdministrativeContracts.map(pack.purpose(),Purpose.class),target.id(),AdministrativeContracts.receipt(outcome.receipt()),outcome.replayed()));
    }
}

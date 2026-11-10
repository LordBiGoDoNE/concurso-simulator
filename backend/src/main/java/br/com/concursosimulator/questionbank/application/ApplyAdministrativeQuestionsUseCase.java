package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.*;
import java.time.*;
import java.util.*;

public final class ApplyAdministrativeQuestionsUseCase {
    private final QuestionImportStore store;
    private final Clock clock;
    public ApplyAdministrativeQuestionsUseCase(QuestionImportStore store,Clock clock){this.store=store;this.clock=clock;}
    public record Outcome(ImportReceipt receipt,boolean replayed) {}
    public static final class ConfirmationFailure extends RuntimeException {
        private final boolean expired;
        public ConfirmationFailure(boolean expired){super("Confirmação inválida");this.expired=expired;}
        public boolean expired(){return expired;}
    }
    public Outcome execute(QuestionImportPackage pack,ImportConfirmation confirmation,UUID actor,String destination) {
        if(!confirmation.actor().equals(actor) || !confirmation.destination().equals(destination)
                || !confirmation.packageDigest().equals(pack.digest()) || !confirmation.selectionDigest().equals(pack.selectionDigest()))
            throw new ConfirmationFailure(false);
        var now=clock.instant();
        if(confirmation.issuedAt().isAfter(now) || !confirmation.expiresAt().isAfter(now)
                || !confirmation.expiresAt().equals(confirmation.issuedAt().plusSeconds(900)))throw new ConfirmationFailure(true);
        try{return apply(pack,confirmation);}
        catch(OriginAlreadyExistsException | QuestionImportStore.Collision conflict){return apply(pack,confirmation);}
    }
    private Outcome apply(QuestionImportPackage pack,ImportConfirmation confirmation){
        return store.importTransaction(session->{
            var previous=session.receipt(confirmation.nonce());
            if(previous.isPresent()) {
                if(!previous.get().confirmation().equals(confirmation))throw new ConfirmationFailure(false);
                return new Outcome(previous.get(),true);
            }
            var results=ApplyQuestionCandidatesUseCase.applyInSession(pack.candidates(),session);
            var receipt=new ImportReceipt(confirmation,clock.instant(),results);
            session.saveImport(receipt,pack.evidence());
            return new Outcome(receipt,false);
        });
    }
}

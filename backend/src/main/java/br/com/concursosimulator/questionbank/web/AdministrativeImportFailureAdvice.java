package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.ApplyAdministrativeQuestionsUseCase.ConfirmationFailure;
import br.com.concursosimulator.questionbank.application.port.StaleQuestionException;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader.InvalidPackage;
import br.com.concursosimulator.shared.failure.TransientFailures;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestControllerAdvice(assignableTypes=AdministrativeImportController.class)
public class AdministrativeImportFailureAdvice {
    public record ErrorResponse(String error,String correlationId) {}
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(AdministrativeImportFailureAdvice.class);
    private ResponseEntity<ErrorResponse> error(int status,String code){
        var correlation=UUID.randomUUID().toString();LOG.warn("Question import rejected code={} correlationId={}",code,correlation);
        return ResponseEntity.status(status).header("Cache-Control","no-store").body(new ErrorResponse(code,correlation));
    }
    @ExceptionHandler(InvalidPackage.class) ResponseEntity<ErrorResponse> invalid(){return error(422,"INVALID_PACKAGE");}
    @ExceptionHandler(ConfirmationFailure.class) ResponseEntity<ErrorResponse> confirmation(ConfirmationFailure failure){return error(409,failure.expired()?"CONFIRMATION_EXPIRED":"CONFIRMATION_MISMATCH");}
    @ExceptionHandler(StaleQuestionException.class) ResponseEntity<ErrorResponse> stale(){return error(409,"STALE_BASE");}
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ErrorResponse> malformed(){return error(400,"INVALID_REQUEST");}
    @ExceptionHandler(ImportRequestBodyAdvice.TooLarge.class) ResponseEntity<ErrorResponse> large(){return error(413,"REQUEST_TOO_LARGE");}
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class) ResponseEntity<ErrorResponse> media(){return error(415,"UNSUPPORTED_MEDIA_TYPE");}
    /** 503 somente para indisponibilidade transitória ({@link TransientFailures}); o restante é falha inesperada. */
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    ResponseEntity<ErrorResponse> data(RuntimeException failure){return TransientFailures.transientCause(failure)?error(503,"IMPORT_UNAVAILABLE"):unexpected();}
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(){return error(500,"IMPORT_FAILED");}
}

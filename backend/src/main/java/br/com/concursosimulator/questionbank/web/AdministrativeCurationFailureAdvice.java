package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries.ExportRefused;
import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries.InvalidQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erros sanitizados e no-store da curadoria administrativa; nunca ecoam conteúdo, SQL ou exceção. */
@RestControllerAdvice(assignableTypes = AdministrativeQuestionController.class)
public class AdministrativeCurationFailureAdvice {
    public record ErrorResponse(String error, String correlationId) {}
    public record ExportErrorResponse(String error, String reason, List<UUID> questionIds, String correlationId) {}
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AdministrativeCurationFailureAdvice.class);
    static String correlation(String code) {
        var correlation = UUID.randomUUID().toString();
        LOG.warn("Administrative curation rejected code={} correlationId={}", code, correlation);
        return correlation;
    }
    static ResponseEntity<ErrorResponse> error(int status, String code) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(new ErrorResponse(code, correlation(code)));
    }
    @ExceptionHandler({InvalidQuery.class, MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> invalid() { return error(400, "INVALID_QUERY"); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ErrorResponse> malformed() { return error(400, "INVALID_REQUEST"); }
    @ExceptionHandler(AdministrativeCurationBodyAdvice.TooLarge.class) ResponseEntity<ErrorResponse> large() { return error(413, "REQUEST_TOO_LARGE"); }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class) ResponseEntity<ErrorResponse> media() { return error(415, "UNSUPPORTED_MEDIA_TYPE"); }
    @ExceptionHandler(AdministrativeQuestionController.QuestionNotFound.class) ResponseEntity<ErrorResponse> missing() { return error(404, "QUESTION_NOT_FOUND"); }
    @ExceptionHandler(ExportRefused.class) ResponseEntity<ExportErrorResponse> refused(ExportRefused refused) {
        return ResponseEntity.status(422).header("Cache-Control", "no-store")
                .body(new ExportErrorResponse("EXPORT_REFUSED", refused.failure().name(), refused.questionIds(), correlation("EXPORT_REFUSED")));
    }
    @ExceptionHandler({org.springframework.dao.TransientDataAccessException.class, org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.CannotCreateTransactionException.class, org.springframework.transaction.TransactionTimedOutException.class})
    ResponseEntity<ErrorResponse> unavailable() { return error(503, "ADMIN_UNAVAILABLE"); }
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected() { return error(500, "ADMIN_CURATION_FAILED"); }
}

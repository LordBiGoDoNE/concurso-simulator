package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.port.UnsafeQuestionProjectionException;
import br.com.concursosimulator.shared.web.ApiError;
import br.com.concursosimulator.shared.web.ApiErrors;
import br.com.concursosimulator.shared.web.InvalidRequest;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.*;

/** Contrato de erro das questões públicas: exceções nomeadas decidem o status; o resto é 500 sem eco. */
@RestControllerAdvice(assignableTypes=QuestionController.class)
public class QuestionFailureAdvice {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(QuestionFailureAdvice.class);

    @ExceptionHandler({InvalidRequest.class,org.springframework.beans.TypeMismatchException.class})
    public ResponseEntity<ApiError> invalid() { return ApiErrors.of(400,"INVALID_REQUEST"); }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> absent() { return ApiErrors.of(404,"NOT_FOUND"); }

    @ExceptionHandler({TransientDataAccessException.class,DataAccessResourceFailureException.class,CannotCreateTransactionException.class,
            TransactionTimedOutException.class,UnsafeQuestionProjectionException.class})
    public ResponseEntity<ApiError> unavailable() { return ApiErrors.of(503,"UNAVAILABLE"); }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> unexpected(RuntimeException failure) {
        LOG.error("Falha não tratada nas questões públicas correlationId={}", UUID.randomUUID(), failure);
        return ApiErrors.of(500,"INTERNAL_ERROR");
    }
}

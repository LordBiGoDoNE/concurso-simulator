package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.application.ParameterizedGenerationUnavailable;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.application.port.UnsafeQuestionProjectionException;
import br.com.concursosimulator.shared.web.ApiError;
import br.com.concursosimulator.shared.web.ApiErrors;
import br.com.concursosimulator.shared.web.InvalidRequest;
import java.util.UUID;
import br.com.concursosimulator.shared.failure.TransientFailures;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

/** Contrato de erro de simulados e práticas: exceções nomeadas decidem o status; o resto é 500 sem eco. */
@RestControllerAdvice(assignableTypes={AttemptController.class,AttemptHistoryController.class,LessonPracticeController.class})
public class AttemptFailureAdvice {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AttemptFailureAdvice.class);

    @ExceptionHandler({InvalidRequest.class,Attempt.InvalidAnswer.class,HttpMessageNotReadableException.class,
            org.springframework.beans.TypeMismatchException.class})
    public ResponseEntity<ApiError> invalid() { return ApiErrors.of(400,"INVALID_REQUEST"); }

    @ExceptionHandler(Attempt.Unavailable.class)
    public ResponseEntity<ApiError> absent() { return ApiErrors.of(404,"NOT_FOUND"); }

    @ExceptionHandler(Attempt.Conflict.class)
    public ResponseEntity<ApiError> conflict() { return ApiErrors.of(409,"CONFLICT"); }

    @ExceptionHandler(Composition.InsufficientPool.class)
    public ResponseEntity<AttemptContracts.Insufficient> insufficient(Composition.InsufficientPool e) {
        return ApiErrors.with(409,new AttemptContracts.Insufficient("INSUFFICIENT_POOL",
                e.available().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                        .map(a -> new AttemptContracts.Available(a.getKey().id(),a.getValue())).toList()));
    }

    @ExceptionHandler(ParameterizedGenerationUnavailable.class)
    public ResponseEntity<ApiError> generationUnavailable() { return ApiErrors.of(503,"PARAMETERIZED_GENERATION_UNAVAILABLE"); }

    @ExceptionHandler(UnsafeQuestionProjectionException.class)
    public ResponseEntity<ApiError> unavailable() { return ApiErrors.of(503,"UNAVAILABLE"); }

    /** Banco/transação: 503 só para indisponibilidade transitória; integridade e uso inválido são 500. */
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    public ResponseEntity<ApiError> data(RuntimeException failure) {
        if (TransientFailures.transientCause(failure)) return ApiErrors.of(503,"UNAVAILABLE");
        return unexpected(failure);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> unexpected(RuntimeException failure) {
        LOG.error("Falha não tratada em simulados/práticas correlationId={}", UUID.randomUUID(), failure);
        return ApiErrors.of(500,"INTERNAL_ERROR");
    }
}

package br.com.concursosimulator.subjectreview.web;

import br.com.concursosimulator.mockexam.domain.Attempt;
import br.com.concursosimulator.shared.failure.TransientFailures;
import br.com.concursosimulator.shared.web.ApiError;
import br.com.concursosimulator.shared.web.ApiErrors;
import br.com.concursosimulator.shared.web.InvalidRequest;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

/** Contrato de erro da revisão por assunto: exceções nomeadas decidem o status; o resto é 500 sem eco. */
@RestControllerAdvice(assignableTypes = ReviewController.class)
public class ReviewFailureAdvice {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReviewFailureAdvice.class);

    @ExceptionHandler({InvalidRequest.class, org.springframework.beans.TypeMismatchException.class})
    public ResponseEntity<ApiError> invalid() {
        return ApiErrors.of(400, "INVALID_REQUEST");
    }

    @ExceptionHandler(Attempt.Unavailable.class)
    public ResponseEntity<ApiError> absent() {
        return ApiErrors.of(404, "NOT_FOUND");
    }

    @ExceptionHandler(Attempt.Conflict.class)
    public ResponseEntity<ApiError> conflict() {
        return ApiErrors.of(409, "CONFLICT");
    }

    /** Banco/transação: 503 só para indisponibilidade transitória; integridade e uso inválido são 500. */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ApiError> data(RuntimeException failure) {
        if (TransientFailures.transientCause(failure)) return ApiErrors.of(503, "UNAVAILABLE");
        return unexpected(failure);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> unexpected(RuntimeException failure) {
        LOG.error("Falha não tratada na revisão por assunto correlationId={}", UUID.randomUUID(), failure);
        return ApiErrors.of(500, "INTERNAL_ERROR");
    }
}

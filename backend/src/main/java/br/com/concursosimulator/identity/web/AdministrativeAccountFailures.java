package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Failure;
import br.com.concursosimulator.shared.web.ApiError;
import br.com.concursosimulator.shared.web.ApiErrors;
import br.com.concursosimulator.shared.failure.TransientFailures;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

/** Contrato de erro da conta administrativa: falhas nomeadas da aplicação decidem o status. */
@RestControllerAdvice(assignableTypes=AdministrativeAccountController.class)
public class AdministrativeAccountFailures {
    @ExceptionHandler(Failure.class) ResponseEntity<ApiError> failure(Failure failure) {
        int status=switch(failure.code()) {
            case "LOGIN_THROTTLED"->429;case "INVALID_CREDENTIALS"->401;case "ADMIN_ACCESS_DENIED"->403;case "INVALID_REQUEST"->400;
            default->409;
        };
        var response=ApiErrors.of(status,failure.code());
        return status==429?ResponseEntity.status(429).headers(response.getHeaders()).header("Retry-After","900").body(response.getBody()):response;
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalid() { return ApiErrors.of(400,"INVALID_REQUEST"); }
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    ResponseEntity<ApiError> data(RuntimeException failure) {
        return TransientFailures.transientCause(failure) ? ApiErrors.of(503,"ADMIN_UNAVAILABLE") : unexpected(failure);
    }
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiError> unexpected(RuntimeException failure) { return ApiErrors.of(500,"ADMIN_FAILURE"); }
}

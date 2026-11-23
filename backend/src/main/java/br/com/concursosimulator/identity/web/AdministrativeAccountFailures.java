package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Failure;
import br.com.concursosimulator.shared.web.ApiError;
import br.com.concursosimulator.shared.web.ApiErrors;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
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
    @ExceptionHandler({TransientDataAccessException.class,DataAccessResourceFailureException.class,CannotCreateTransactionException.class,
            TransactionTimedOutException.class})
    ResponseEntity<ApiError> unavailable() { return ApiErrors.of(503,"ADMIN_UNAVAILABLE"); }
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiError> unexpected(RuntimeException failure) { return ApiErrors.of(500,"ADMIN_FAILURE"); }
}

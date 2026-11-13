package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Failure;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=AdministrativeAccountController.class)
public class AdministrativeAccountFailures {
    public record ErrorResponse(String error) {}
    @ExceptionHandler(Failure.class) ResponseEntity<ErrorResponse> failure(Failure failure) {
        int status=switch(failure.code()) { case "LOGIN_THROTTLED"->429;case "INVALID_CREDENTIALS"->401;case "GOOGLE_DISABLED"->409;case "ADMIN_ACCESS_DENIED"->403;default->409; };
        var response=ResponseEntity.status(status).header("Cache-Control","no-store");if(status==429)response.header("Retry-After","900");
        return response.body(new ErrorResponse(failure.code()));
    }
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalid(Exception failure) { return ResponseEntity.badRequest().header("Cache-Control","no-store").body(new ErrorResponse("INVALID_REQUEST")); }
    @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})
    ResponseEntity<ErrorResponse> unavailable(Exception failure) { return ResponseEntity.status(503).header("Cache-Control","no-store").body(new ErrorResponse("ADMIN_UNAVAILABLE")); }
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ErrorResponse> unexpected(RuntimeException failure) { return ResponseEntity.internalServerError().header("Cache-Control","no-store").body(new ErrorResponse("ADMIN_FAILURE")); }
}

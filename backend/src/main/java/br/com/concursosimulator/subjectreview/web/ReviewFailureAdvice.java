package br.com.concursosimulator.subjectreview.web;

import br.com.concursosimulator.mockexam.domain.Attempt;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=ReviewController.class)
public class ReviewFailureAdvice {
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ReviewContracts.Error> invalid() { return error(400,"invalid_request"); }
    @ExceptionHandler(Attempt.Unavailable.class) public ResponseEntity<ReviewContracts.Error> absent() { return error(404,"not_found"); }
    @ExceptionHandler(Attempt.Conflict.class) public ResponseEntity<ReviewContracts.Error> conflict() { return error(409,"conflict"); }
    @ExceptionHandler({IllegalStateException.class,org.springframework.dao.InvalidDataAccessApiUsageException.class})
    public ResponseEntity<ReviewContracts.Error> integrity() { return error(500,"internal_error"); }
    @ExceptionHandler({DataAccessException.class,TransactionException.class}) public ResponseEntity<ReviewContracts.Error> unavailable() { return error(503,"unavailable"); }
    private static ResponseEntity<ReviewContracts.Error> error(int status,String code) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ReviewContracts.Error(code)); }
}

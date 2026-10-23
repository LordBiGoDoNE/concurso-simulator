package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.application.port.UnsafeQuestionProjectionException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes={AttemptController.class,AttemptHistoryController.class})
public class AttemptFailureAdvice {
    @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<AttemptContracts.Error> invalid() { return error(400,"invalid_request"); }
    @ExceptionHandler(Attempt.Unavailable.class) public ResponseEntity<AttemptContracts.Error> absent() { return error(404,"not_found"); }
    @ExceptionHandler(Attempt.Conflict.class) public ResponseEntity<AttemptContracts.Error> conflict() { return error(409,"conflict"); }
    @ExceptionHandler({IllegalStateException.class,org.springframework.dao.InvalidDataAccessApiUsageException.class})
    public ResponseEntity<AttemptContracts.Error> integrity() { return error(500,"internal_error"); }
    @ExceptionHandler(Composition.InsufficientPool.class) public ResponseEntity<AttemptContracts.Insufficient> insufficient(Composition.InsufficientPool e) {
        return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).body(new AttemptContracts.Insufficient("insufficient_pool",
                e.available().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(a -> new AttemptContracts.Available(a.getKey().id(),a.getValue())).toList()));
    }
    @ExceptionHandler({DataAccessException.class,TransactionException.class,UnsafeQuestionProjectionException.class})
    public ResponseEntity<AttemptContracts.Error> unavailable() { return error(503,"unavailable"); }
    private static ResponseEntity<AttemptContracts.Error> error(int status,String code) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new AttemptContracts.Error(code)); }
}

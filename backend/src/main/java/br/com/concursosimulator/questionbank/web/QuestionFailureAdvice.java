package br.com.concursosimulator.questionbank.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import java.util.NoSuchElementException;
import br.com.concursosimulator.questionbank.application.port.UnsafeQuestionProjectionException;

@RestControllerAdvice(assignableTypes=QuestionController.class)
public class QuestionFailureAdvice {
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<QuestionResponses.Error> invalid() { return error(400,"invalid_request"); }
    @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<QuestionResponses.Error> absent() { return error(404,"not_found"); }
    @ExceptionHandler({DataAccessException.class,TransactionException.class,UnsafeQuestionProjectionException.class})
    public ResponseEntity<QuestionResponses.Error> unavailable() { return error(503,"unavailable"); }
    private static ResponseEntity<QuestionResponses.Error> error(int status,String code) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new QuestionResponses.Error(code)); }
}

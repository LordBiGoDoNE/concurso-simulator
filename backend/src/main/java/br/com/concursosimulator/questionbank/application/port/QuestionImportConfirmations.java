package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.*;
import java.time.Instant;
import java.util.UUID;

public interface QuestionImportConfirmations {
    record Issued(String token,Instant expiresAt) {}
    Issued issue(UUID actor,String destination,QuestionImportPackage pack);
    ImportConfirmation verify(String token);
}

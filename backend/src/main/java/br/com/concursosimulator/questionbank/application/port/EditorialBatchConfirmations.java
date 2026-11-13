package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.BatchConfirmation;
import java.time.Instant;
import java.util.UUID;

/** Emissão/verificação do token da prévia de decisões em lote; separado de autenticação e CSRF. */
public interface EditorialBatchConfirmations {
    record Issued(String token, Instant expiresAt) {}
    Issued issue(UUID actor, String destination, String digest);
    BatchConfirmation verify(String token);
}

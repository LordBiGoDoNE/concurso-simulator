package br.com.concursosimulator.mockexam.application;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import java.time.Instant;
import java.util.*;

/** Fronteira de leitura de mockexam; confirma posse/estado e integridade antes de projetar. */
public interface CompletedAttemptReader {
    int MAX_LIMIT=100;
    record Window(List<CompletedAttempt> attempts, boolean hasOlder) {
        public Window {
            attempts=List.copyOf(attempts);
            if (attempts.size()>MAX_LIMIT || hasOlder && attempts.isEmpty())
                throw new IllegalArgumentException("Janela de evidência inválida");
        }
    }
    /** Posse/cargo/conclusão e criação<=now antecedem limite; ordem finishedAt/UUID sem sinal; uma visão transacional. */
    Window recent(AttemptOwner account, TargetRole role, int limit, Instant now);
    /** Ausente/alheia/expirada: Unavailable; própria aberta: Conflict; nunca retorna correção privada. */
    CompletedAttempt one(AttemptOwner actor, UUID id, Instant now);
}

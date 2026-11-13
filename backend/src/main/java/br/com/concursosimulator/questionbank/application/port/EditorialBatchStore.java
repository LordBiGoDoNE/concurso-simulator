package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Decisões do lote e recibo na MESMA unidade; retorno somente após commit/rollback. */
public interface EditorialBatchStore {
    <T> T batchTransaction(Function<Session, T> operation);
    interface Session extends QuestionStore.Session {
        Optional<EditorialBatchUseCase.Receipt> batchReceipt(UUID nonce);
        void saveBatch(EditorialBatchUseCase.Receipt receipt);
    }
    /** Recibo do mesmo nonce gravado concorrentemente; a unidade anterior terminou em rollback. */
    final class Collision extends RuntimeException {
        public Collision(Throwable cause) { super("Colisão de lote editorial", cause); }
    }
}

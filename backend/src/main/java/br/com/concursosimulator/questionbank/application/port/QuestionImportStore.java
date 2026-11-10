package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.*;
import java.util.*;
import java.util.function.Function;

/** Questões, evidência e recibo na MESMA unidade; retorno somente após commit/rollback. */
public interface QuestionImportStore {
    <T> T importTransaction(Function<Session,T> operation);
    interface Session extends QuestionStore.Session {
        Optional<ImportReceipt> receipt(UUID nonce);
        void saveImport(ImportReceipt receipt, List<QuestionImportPackage.Evidence> evidence);
    }
    final class Collision extends RuntimeException {
        public Collision(Throwable cause){super("Colisão de importação",cause);}
    }
}

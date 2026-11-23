package br.com.concursosimulator.shared.failure;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;

/**
 * Falhas que justificam 503: banco, conexão ou transação temporariamente indisponíveis. Violação de
 * integridade e uso inválido da API de dados não são indisponibilidade.
 *
 * <p>Quando a conexão cai no meio da transação, o rollback também falha e a falha original pode se perder:
 * o que chega é uma falha do sistema de transação (Spring {@link TransactionSystemException} ou
 * {@code org.hibernate.TransactionException}, por exemplo "Unable to rollback ... Connection is closed").
 * Essa falha é tratada como indisponibilidade, salvo se a cadeia trouxer violação de integridade (SQLState 23).
 * Pacote neutro: usado pelos adaptadores web (advices) e pelos filtros de infraestrutura.
 */
public final class TransientFailures {
    private static final String HIBERNATE_TRANSACTION_FAILURE = "org.hibernate.TransactionException";
    private TransientFailures() {}

    public static boolean transientCause(Throwable failure) {
        if (integrity(failure, Collections.newSetFromMap(new IdentityHashMap<>()))) return false;
        return transientCause(failure, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean transientCause(Throwable failure, Set<Throwable> seen) {
        for (var cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (isTransient(cause) || transactionSystemFailure(cause)) return true;
            if (cause instanceof TransactionSystemException system && system.getOriginalException() != null
                    && system.getOriginalException() != system && transientCause(system.getOriginalException(), seen)) return true;
        }
        return false;
    }

    private static boolean integrity(Throwable failure, Set<Throwable> seen) {
        for (var cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException) return true;
            if (cause instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23")) return true;
            if (cause instanceof TransactionSystemException system && system.getOriginalException() != null
                    && system.getOriginalException() != system && integrity(system.getOriginalException(), seen)) return true;
        }
        return false;
    }

    private static boolean transactionSystemFailure(Throwable failure) {
        return failure instanceof TransactionSystemException || HIBERNATE_TRANSACTION_FAILURE.equals(failure.getClass().getName());
    }

    public static boolean isTransient(Throwable failure) {
        return failure instanceof TransientDataAccessException || failure instanceof DataAccessResourceFailureException
                || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException
                || failure instanceof SQLTransientConnectionException
                // SQLState classe 08: exceção de conexão (padrão SQL), sem ligação com integridade.
                || failure instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08");
    }
}

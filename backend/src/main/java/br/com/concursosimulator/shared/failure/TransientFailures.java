package br.com.concursosimulator.shared.failure;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;
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
 * <p>Transitório, em qualquer ponto da cadeia de causas: {@link TransientDataAccessException},
 * {@link DataAccessResourceFailureException}, {@link CannotCreateTransactionException},
 * {@link TransactionTimedOutException} e falha de conexão JDBC ({@link SQLTransientConnectionException},
 * {@link SQLRecoverableException}, {@link SQLNonTransientConnectionException} ou SQLState classe 08).
 *
 * <p>Falha do sistema de transação (Spring {@link TransactionSystemException} ou
 * {@code org.hibernate.TransactionException}) só é transitória quando a própria cadeia dela mostra a conexão
 * perdida: uma das falhas de conexão acima ou a conexão fechada do pool ({@code SQLException} "Connection is
 * closed", sem SQLState, lançada pelo HikariCP), que é o que chega quando a conexão cai no meio da transação e o
 * rollback também falha ({@code JpaSystemException} → "Unable to rollback against JDBC Connection" → "Connection
 * is closed"). Sem essa evidência (por exemplo "Transaction already active", erro de programação) é 500.
 * Violação de integridade (SQLState 23) em qualquer ponto da cadeia nunca é transitória.
 * Pacote neutro: usado pelos adaptadores web (advices) e pelos filtros de infraestrutura.
 */
public final class TransientFailures {
    private static final String HIBERNATE_TRANSACTION_FAILURE = "org.hibernate.TransactionException";
    private static final String POOL_CLOSED_CONNECTION = "Connection is closed";
    private TransientFailures() {}

    public static boolean transientCause(Throwable failure) {
        if (anyInChain(failure, TransientFailures::integrity)) return false;
        return anyInChain(failure, cause -> isTransient(cause)
                || transactionSystemFailure(cause) && anyInChain(cause, TransientFailures::connectionLost));
    }

    public static boolean isTransient(Throwable failure) {
        return failure instanceof TransientDataAccessException || failure instanceof DataAccessResourceFailureException
                || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException
                || connectionFailure(failure);
    }

    private static boolean connectionFailure(Throwable failure) {
        return failure instanceof SQLTransientConnectionException || failure instanceof SQLRecoverableException
                || failure instanceof SQLNonTransientConnectionException
                // SQLState classe 08: exceção de conexão (padrão SQL), sem ligação com integridade.
                || failure instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08");
    }

    private static boolean connectionLost(Throwable failure) {
        return connectionFailure(failure) || failure instanceof SQLException sql && sql.getSQLState() == null
                && POOL_CLOSED_CONNECTION.equals(sql.getMessage());
    }

    private static boolean integrity(Throwable failure) {
        return failure instanceof DataIntegrityViolationException
                || failure instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23");
    }

    private static boolean transactionSystemFailure(Throwable failure) {
        return failure instanceof TransactionSystemException || HIBERNATE_TRANSACTION_FAILURE.equals(failure.getClass().getName());
    }

    /** Percorre causas e, em {@link TransactionSystemException}, a exceção original que o rollback encobriu. */
    private static boolean anyInChain(Throwable failure, Predicate<Throwable> test) {
        return anyInChain(failure, test, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean anyInChain(Throwable failure, Predicate<Throwable> test, Set<Throwable> seen) {
        for (var cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (test.test(cause)) return true;
            if (cause instanceof TransactionSystemException system && system.getOriginalException() != null
                    && anyInChain(system.getOriginalException(), test, seen)) return true;
        }
        return false;
    }
}

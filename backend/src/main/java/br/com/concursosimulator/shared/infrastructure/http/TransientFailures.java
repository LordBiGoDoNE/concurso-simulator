package br.com.concursosimulator.shared.infrastructure.http;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;

/**
 * Falhas que justificam 503: banco, conexão ou transação temporariamente indisponíveis. Violação de
 * integridade e uso inválido da API de dados não são indisponibilidade.
 */
public final class TransientFailures {
    private TransientFailures() {}

    public static boolean transientCause(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) if (isTransient(cause)) return true;
        return false;
    }

    public static boolean isTransient(Throwable failure) {
        return failure instanceof TransientDataAccessException || failure instanceof DataAccessResourceFailureException
                || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException;
    }
}

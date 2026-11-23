package br.com.concursosimulator.platform;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.shared.failure.TransientFailures;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

class TransientFailuresTests {
    @Test
    void outagesAreTransient() {
        assertThat(TransientFailures.transientCause(new CannotCreateTransactionException("tx")))
                .isTrue();
        assertThat(TransientFailures.transientCause(new QueryTimeoutException("timeout")))
                .isTrue();
        assertThat(TransientFailures.transientCause(
                        new RuntimeException(new DataAccessResourceFailureException("down"))))
                .isTrue();
        assertThat(TransientFailures.transientCause(new IllegalStateException(new SQLException("refused", "08001"))))
                .isTrue();
        assertThat(TransientFailures.transientCause(new SQLTransientConnectionException("pool timeout")))
                .isTrue();
        assertThat(TransientFailures.transientCause(new SQLRecoverableException("recoverable")))
                .isTrue();
        assertThat(TransientFailures.transientCause(new SQLNonTransientConnectionException("refused")))
                .isTrue();
    }

    @Test
    void connectionLostDuringTransactionIsTransientEvenWhenTheOriginalIsLost() {
        // Caminho observado com o PostgreSQL parado no meio da requisição.
        var rollback = new JpaSystemException(new RuntimeException(new org.hibernate.TransactionException(
                "Unable to rollback against JDBC Connection", new SQLException("Connection is closed"))));
        assertThat(TransientFailures.transientCause(rollback)).isTrue();
        var spring = new TransactionSystemException("rollback failed", new SQLException("I/O"));
        spring.initApplicationException(new DataAccessResourceFailureException("connection lost"));
        assertThat(TransientFailures.transientCause(spring)).isTrue();
        assertThat(TransientFailures.transientCause(
                        new TransactionSystemException("rollback failed", new SQLException("I/O", "08006"))))
                .isTrue();
    }

    @Test
    void transactionSystemFailureWithoutLostConnectionIsNotUnavailability() {
        // Erro de programação do Hibernate: não há conexão perdida na cadeia.
        assertThat(TransientFailures.transientCause(
                        new JpaSystemException(new org.hibernate.TransactionException("Transaction already active"))))
                .isFalse();
        assertThat(TransientFailures.transientCause(new TransactionSystemException(
                        "Could not commit JPA transaction", new IllegalStateException("Transaction not active"))))
                .isFalse();
        assertThat(TransientFailures.transientCause(
                        new TransactionSystemException("rollback failed", new SQLException("I/O"))))
                .isFalse();
        // Mensagem do pool só conta sem SQLState e dentro de falha do sistema de transação.
        assertThat(TransientFailures.transientCause(
                        new IllegalStateException(new SQLException("Connection is closed"))))
                .isFalse();
        assertThat(TransientFailures.transientCause(new org.hibernate.TransactionException(
                        "Unable to rollback against JDBC Connection",
                        new SQLException("Connection is closed", "42000"))))
                .isFalse();
    }

    @Test
    void integrityAndMisuseAreNotUnavailability() {
        assertThat(TransientFailures.transientCause(
                        new DataIntegrityViolationException("constraint", new SQLException("dup", "23505"))))
                .isFalse();
        assertThat(TransientFailures.transientCause(
                        new TransactionSystemException("commit failed", new SQLException("deferred", "23503"))))
                .isFalse();
        assertThat(TransientFailures.transientCause(new InvalidDataAccessApiUsageException("misuse")))
                .isFalse();
        assertThat(TransientFailures.transientCause(new IllegalStateException("inconsistent")))
                .isFalse();
    }
}

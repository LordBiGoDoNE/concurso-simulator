package br.com.concursosimulator.platform;

import br.com.concursosimulator.shared.failure.TransientFailures;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import static org.assertj.core.api.Assertions.assertThat;

class TransientFailuresTests {
    @Test
    void outagesAreTransient() {
        assertThat(TransientFailures.transientCause(new CannotCreateTransactionException("tx"))).isTrue();
        assertThat(TransientFailures.transientCause(new QueryTimeoutException("timeout"))).isTrue();
        assertThat(TransientFailures.transientCause(new RuntimeException(new DataAccessResourceFailureException("down")))).isTrue();
        assertThat(TransientFailures.transientCause(new IllegalStateException(new SQLException("refused", "08001")))).isTrue();
    }

    @Test
    void connectionLostDuringTransactionIsTransientEvenWhenTheOriginalIsLost() {
        // Caminho observado com o PostgreSQL parado no meio da requisição.
        var rollback = new JpaSystemException(new RuntimeException(new org.hibernate.TransactionException("Unable to rollback against JDBC Connection",
                new SQLException("Connection is closed"))));
        assertThat(TransientFailures.transientCause(rollback)).isTrue();
        var spring = new TransactionSystemException("rollback failed", new SQLException("I/O"));
        spring.initApplicationException(new DataAccessResourceFailureException("connection lost"));
        assertThat(TransientFailures.transientCause(spring)).isTrue();
    }

    @Test
    void integrityAndMisuseAreNotUnavailability() {
        assertThat(TransientFailures.transientCause(new DataIntegrityViolationException("constraint", new SQLException("dup", "23505")))).isFalse();
        assertThat(TransientFailures.transientCause(new TransactionSystemException("commit failed", new SQLException("deferred", "23503")))).isFalse();
        assertThat(TransientFailures.transientCause(new InvalidDataAccessApiUsageException("misuse"))).isFalse();
        assertThat(TransientFailures.transientCause(new IllegalStateException("inconsistent"))).isFalse();
    }
}

package br.com.concursosimulator.questionbank.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

/** Decisão 503 x 500 dos advices administrativos: 503 somente para indisponibilidade transitória. */
class AdministrativeFailureAdvicesTests {
    private final AdministrativeCurationFailureAdvice curation = new AdministrativeCurationFailureAdvice();
    private final AdministrativeImportFailureAdvice importing = new AdministrativeImportFailureAdvice();

    /** Conexão perdida no meio da transação: o rollback falha e a falha original se perde. */
    private static RuntimeException rollbackWithLostConnection() {
        return new JpaSystemException(new RuntimeException(new org.hibernate.TransactionException(
                "Unable to rollback against JDBC Connection", new SQLException("Connection is closed"))));
    }

    @Test
    void curationAnswers503OnlyForTransientUnavailability() {
        assertCuration(curation.data(rollbackWithLostConnection()), 503, "ADMIN_UNAVAILABLE");
        assertCuration(curation.data(new TransactionSystemException("rollback failed", new SQLException("I/O", "08006"))), 503, "ADMIN_UNAVAILABLE");
        assertCuration(curation.data(new CannotCreateTransactionException("tx")), 503, "ADMIN_UNAVAILABLE");
        assertCuration(curation.data(new QueryTimeoutException("timeout")), 503, "ADMIN_UNAVAILABLE");
        assertCuration(curation.data(new DataIntegrityViolationException("dup", new SQLException("dup", "23505"))), 500, "ADMIN_CURATION_FAILED");
        assertCuration(curation.data(new TransactionSystemException("commit failed", new SQLException("deferred", "23503"))), 500, "ADMIN_CURATION_FAILED");
        assertCuration(curation.data(new InvalidDataAccessApiUsageException("misuse")), 500, "ADMIN_CURATION_FAILED");
    }

    @Test
    void importAnswers503OnlyForTransientUnavailability() {
        assertImport(importing.data(rollbackWithLostConnection()), 503, "IMPORT_UNAVAILABLE");
        assertImport(importing.data(new TransactionSystemException("rollback failed", new SQLException("I/O", "08006"))), 503, "IMPORT_UNAVAILABLE");
        assertImport(importing.data(new CannotCreateTransactionException("tx")), 503, "IMPORT_UNAVAILABLE");
        assertImport(importing.data(new DataIntegrityViolationException("dup", new SQLException("dup", "23505"))), 500, "IMPORT_FAILED");
        assertImport(importing.data(new TransactionSystemException("commit failed", new SQLException("deferred", "23503"))), 500, "IMPORT_FAILED");
        assertImport(importing.data(new InvalidDataAccessApiUsageException("misuse")), 500, "IMPORT_FAILED");
    }

    private static void assertCuration(ResponseEntity<AdministrativeCurationFailureAdvice.ErrorResponse> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().error()).isEqualTo(code);
        assertThat(response.getBody().correlationId()).isNotBlank();
    }

    private static void assertImport(ResponseEntity<AdministrativeImportFailureAdvice.ErrorResponse> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody().error()).isEqualTo(code);
        assertThat(response.getBody().correlationId()).isNotBlank();
    }
}

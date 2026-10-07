package br.com.concursosimulator.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GoogleIdentityService {
    private static final String PROVIDER = "google";
    private static final String ISSUER = "https://accounts.google.com";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public GoogleIdentityService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Deve ser chamado somente após a validação OIDC, nunca por input público. */
    public UserPrincipal resolveVerifiedSubject(String subject) {
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new IllegalArgumentException("Subject Google inválido");
        }
        try {
            return transaction.execute(status -> {
                var existing = find(subject);
                if (existing.isPresent()) return new UserPrincipal(existing.get());
                var id = UUID.randomUUID();
                jdbc.update("INSERT INTO app_user (id) VALUES (?)", id);
                jdbc.update("INSERT INTO external_identity (user_id, provider, issuer, subject) VALUES (?, ?, ?, ?)",
                        id, PROVIDER, ISSUER, subject);
                return new UserPrincipal(id);
            });
        } catch (DuplicateKeyException exception) {
            // A corrida pela identidade perde a transação inteira, incluindo o usuário novo.
            // A leitura ocorre depois do rollback, em nova transação, não na transação abortada.
            return transaction.execute(status -> new UserPrincipal(find(subject).orElseThrow(() -> exception)));
        }
    }

    private Optional<UUID> find(String subject) {
        return jdbc.query("SELECT user_id FROM external_identity WHERE provider = ? AND issuer = ? AND subject = ?",
                (row, number) -> row.getObject("user_id", UUID.class), PROVIDER, ISSUER, subject)
                .stream().findFirst();
    }
}

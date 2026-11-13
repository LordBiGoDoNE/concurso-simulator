package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.application.BatchConfirmation;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase;
import br.com.concursosimulator.questionbank.application.port.EditorialBatchConfirmations;
import java.io.*;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token HMAC da prévia de decisões em lote (#177). Usa a chave de confirmação administrativa com
 * separação de domínio própria: um token de importação nunca é aceito aqui, nem o inverso.
 */
public final class EditorialBatchTokens implements EditorialBatchConfirmations {
    private static final String DOMAIN = "editorial-batch-confirmation/v1";
    private final byte[] key; private final Clock clock;
    public EditorialBatchTokens(byte[] key, Clock clock) { this.key = key.clone(); this.clock = clock; }

    @Override public Issued issue(UUID actor, String destination, String digest) {
        var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var c = new BatchConfirmation(UUID.randomUUID(), actor, destination, digest, now, now.plusSeconds(EditorialBatchUseCase.TTL_SECONDS));
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeUTF(DOMAIN); out.writeUTF(c.nonce().toString()); out.writeUTF(c.actor().toString()); out.writeUTF(c.destination());
                out.writeUTF(c.digest()); out.writeLong(c.issuedAt().getEpochSecond()); out.writeLong(c.expiresAt().getEpochSecond());
            }
            var payload = bytes.toByteArray();
            return new Issued(encode(payload) + "." + encode(mac(payload)), c.expiresAt());
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }

    @Override public BatchConfirmation verify(String token) {
        try {
            if (token == null || token.length() > 4096) throw new IllegalArgumentException();
            var parts = token.split("\\.", -1); if (parts.length != 2) throw new IllegalArgumentException();
            var payload = Base64.getUrlDecoder().decode(parts[0]); var signature = Base64.getUrlDecoder().decode(parts[1]);
            if (!encode(payload).equals(parts[0]) || !encode(signature).equals(parts[1]) || !MessageDigest.isEqual(mac(payload), signature))
                throw new IllegalArgumentException();
            try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
                if (!DOMAIN.equals(in.readUTF())) throw new IllegalArgumentException();
                var c = new BatchConfirmation(UUID.fromString(in.readUTF()), UUID.fromString(in.readUTF()), in.readUTF(), in.readUTF(),
                        Instant.ofEpochSecond(in.readLong()), Instant.ofEpochSecond(in.readLong()));
                if (in.available() != 0) throw new IllegalArgumentException();
                return c;
            }
        } catch (IllegalArgumentException | IOException invalid) { throw new EditorialBatchUseCase.ConfirmationFailure(false); }
    }

    private byte[] mac(byte[] payload) {
        if (key.length < 32) throw new IllegalStateException("Decisões em lote desabilitadas");
        try {
            var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(DOMAIN.getBytes(java.nio.charset.StandardCharsets.UTF_8)); mac.update((byte) 0);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException error) { throw new IllegalStateException(error); }
    }
    private static String encode(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}

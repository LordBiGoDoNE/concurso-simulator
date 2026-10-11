package br.com.concursosimulator.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ExternalIdentityPersistenceId implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(nullable = false, length = 32)
    private String provider;
    @Column(nullable = false, length = 255)
    private String issuer;
    @Column(nullable = false, length = 255)
    private String subject;

    protected ExternalIdentityPersistenceId() {}

    ExternalIdentityPersistenceId(String provider, String issuer, String subject) {
        this.provider = provider;
        this.issuer = issuer;
        this.subject = subject;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExternalIdentityPersistenceId id && Objects.equals(provider, id.provider)
                && Objects.equals(issuer, id.issuer) && Objects.equals(subject, id.subject);
    }

    @Override
    public int hashCode() { return Objects.hash(provider, issuer, subject); }
}

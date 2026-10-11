package br.com.concursosimulator.identity.domain;

/** Identidade recebida de adaptador autenticado; valida invariantes, não tokens. */
public record ExternalIdentity(String provider, String issuer, String subject) {
    public ExternalIdentity {
        requireValue(provider, 32, "Provider");
        requireValue(issuer, 255, "Issuer");
        requireValue(subject, 255, "Subject");
    }

    private static void requireValue(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(field + " inválido");
        }
    }

    @Override
    public String toString() { return "ExternalIdentity[redacted]"; }
}

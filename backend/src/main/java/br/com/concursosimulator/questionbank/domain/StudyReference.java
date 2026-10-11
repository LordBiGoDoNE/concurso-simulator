package br.com.concursosimulator.questionbank.domain;

import java.net.URI;
import java.time.LocalDate;

/** Referência de estudo; validação sintática não comprova correção/disponibilidade. */
public record StudyReference(String name, URI url, LegalBasis legalBasis) {
    public StudyReference {
        ContentRules.text(name, 1000, "Nome da referência");
        if (url == null || !("https".equals(url.getScheme()) || "http".equals(url.getScheme()))
                || url.getHost() == null || url.getUserInfo() != null || url.toString().length() > 4000) {
            throw new IllegalArgumentException("URL de estudo inválida");
        }
        ContentRules.text(url.toString(), 4000, "URL de estudo");
    }
    public record LegalBasis(String provision, LocalDate checkedOn) {
        public LegalBasis {
            ContentRules.text(provision, 4000, "Dispositivo legal");
            if (checkedOn == null) throw new IllegalArgumentException("Data de conferência obrigatória");
        }
    }
}

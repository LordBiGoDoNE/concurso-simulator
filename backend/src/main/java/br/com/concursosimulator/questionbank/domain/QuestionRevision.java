package br.com.concursosimulator.questionbank.domain;

import java.util.Objects;
import java.util.UUID;

/** Conteúdo e identificação imutáveis; estado editorial será mantido separadamente. */
public record QuestionRevision(UUID id, int number, QuestionContent content) {
    public QuestionRevision {
        Objects.requireNonNull(id); Objects.requireNonNull(content);
        if (number < 1) throw new IllegalArgumentException("Número de revisão inválido");
    }
    public String fingerprint() { return content.fingerprint(); }
}

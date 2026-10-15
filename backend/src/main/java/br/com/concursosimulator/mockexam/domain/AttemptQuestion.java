package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.QuestionContent;
import java.util.Objects;
import java.util.UUID;

/** Captura interna e privada: não é contrato HTTP. O conteúdo é um VO imutável do banco. */
public record AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content) {
    public AttemptQuestion {
        Objects.requireNonNull(questionId); Objects.requireNonNull(revisionId); Objects.requireNonNull(content);
        if (!content.fingerprint().equals(fingerprint)) throw new IllegalArgumentException("Fingerprint divergente");
    }
}

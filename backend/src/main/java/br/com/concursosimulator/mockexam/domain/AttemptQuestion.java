package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.QuestionContent;
import java.util.Objects;
import java.util.UUID;

/** Captura interna e privada: não é contrato HTTP. O conteúdo é um VO imutável do banco. */
public record AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content, QuestionSource source) {
    public AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content) {
        this(questionId,revisionId,fingerprint,content,QuestionSource.fixed());
    }
    public AttemptQuestion {
        Objects.requireNonNull(source);
        Objects.requireNonNull(questionId); Objects.requireNonNull(revisionId); Objects.requireNonNull(content);
        if (!content.fingerprint().equals(fingerprint)) throw new IllegalArgumentException("Fingerprint divergente");
        if (source.capture()!=null && (!source.capture().baseRevisionId().equals(revisionId)
                || !source.capture().renderedFingerprint().equals(fingerprint)
                || source.capture().parameters().family().topic()!=content.classification().topic()))
            throw new IllegalArgumentException("Fonte/conteúdo divergente");
    }
}

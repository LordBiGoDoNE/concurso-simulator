package br.com.concursosimulator.mockexam.domain;

import br.com.concursosimulator.questionbank.domain.QuestionContent;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import br.com.concursosimulator.parameterization.domain.CapturedOutputDigest;
import br.com.concursosimulator.parameterization.domain.TemplateDefinition;
import java.util.Objects;
import java.util.UUID;

/** Captura interna e privada: não é contrato HTTP. O conteúdo é um VO imutável do banco. */
public record AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content, QuestionSource source, QuestionPurpose purpose) {
    public AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content, QuestionSource source) {
        this(questionId,revisionId,fingerprint,content,source,QuestionPurpose.SIMULADO);
    }
    public AttemptQuestion(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content) {
        this(questionId,revisionId,fingerprint,content,QuestionSource.fixed(),QuestionPurpose.SIMULADO);
    }
    public AttemptQuestion {
        Objects.requireNonNull(source); Objects.requireNonNull(purpose);
        if (purpose==QuestionPurpose.PRATICA_AULA && source.kind()!=QuestionSource.Kind.FIXED)
            throw new IllegalArgumentException("Prática utiliza questões fixas próprias");
        Objects.requireNonNull(questionId); Objects.requireNonNull(revisionId); Objects.requireNonNull(content);
        if (!content.fingerprint().equals(fingerprint)) throw new IllegalArgumentException("Fingerprint divergente");
        if (source.capture()!=null && (!source.capture().baseRevisionId().equals(revisionId)
                || !source.capture().renderedFingerprint().equals(fingerprint)
                || source.capture().parameters().family().topic()!=content.classification().topic()))
            throw new IllegalArgumentException("Fonte/conteúdo divergente");
        if (source.capture()!=null && !source.capture().outputDigest().equals(CapturedOutputDigest.fromContent(
                source.capture().definitionDigest(),source.capture().parameters(),content)))
            throw new IllegalArgumentException("Conteúdo capturado diverge do digest da saída");
        if (source.capture()!=null) {
            var c=source.capture();
            var definition=new TemplateDefinition(c.templateId(),c.templateVersion(),c.parameters().family(),c.generatorVersion(),
                    questionId,c.baseRevisionId(),c.baseFingerprint(),content.classification(),content.references(),content.provenance());
            if (!definition.digest().equals(c.definitionDigest()))
                throw new IllegalArgumentException("Metadados capturados divergem da definição aprovada");
        }
    }
}

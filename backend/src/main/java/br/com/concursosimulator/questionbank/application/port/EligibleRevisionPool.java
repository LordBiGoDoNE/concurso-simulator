package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.domain.QuestionContent;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import java.util.*;

/** Contrato interno privado; participa da transação do consumidor, nunca da API pública. */
public interface EligibleRevisionPool {
    record Entry(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content, QuestionPurpose purpose) {
        public Entry(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content) {
            this(questionId,revisionId,fingerprint,content,QuestionPurpose.SIMULADO);
        }
        public Entry { Objects.requireNonNull(purpose); }
    }
    List<Entry> capture();
    default List<Entry> capture(QuestionPurpose purpose) {
        return capture().stream().filter(e -> e.purpose()==purpose).toList();
    }
}

package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.domain.QuestionContent;
import java.util.*;

/** Contrato interno privado; participa da transação do consumidor, nunca da API pública. */
public interface EligibleRevisionPool {
    record Entry(UUID questionId, UUID revisionId, String fingerprint, QuestionContent content) {}
    List<Entry> capture();
}

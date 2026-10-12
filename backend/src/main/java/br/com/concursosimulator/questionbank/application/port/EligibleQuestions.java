package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;

/** Leitura projetada: contrato não tem resposta, resolução ou estado editorial. */
public interface EligibleQuestions {
    record Filter(Discipline discipline, Topic topic, TargetRole role, int page, int size) {
        public Filter {
            if (page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw new IllegalArgumentException("Paginação inválida");
            if (topic!=null && discipline!=null && topic.discipline()!=discipline) throw new IllegalArgumentException("Filtro incompatível");
        }
    }
    record Summary(UUID questionId, UUID revisionId, QuestionClassification classification, String statementStart) {}
    record Page(List<Summary> items, int page, int size, long total) { public Page { items=List.copyOf(items); } }
    record Detail(UUID questionId, UUID revisionId, QuestionClassification classification, TextContent statement,
                  List<QuestionContent.Alternative> alternatives, List<StudyReference> references) {
        public Detail { alternatives=List.copyOf(alternatives); references=List.copyOf(references); }
    }
    Page list(Filter filter);
    Optional<Detail> detail(UUID questionId);
}

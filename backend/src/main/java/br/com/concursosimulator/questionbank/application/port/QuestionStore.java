package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.domain.Question;
import br.com.concursosimulator.questionbank.domain.QuestionOrigin;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Unidade atômica: retorna somente após commit; falha causa rollback antes de retornar ao chamador. */
public interface QuestionStore {
    <T> T transaction(Function<Session, T> operation);
    interface Session {
        Optional<Question> loadForUpdate(UUID questionId);
        Optional<Question> findOriginForUpdate(QuestionOrigin origin);
        void insert(Question question);
        void save(Question question);
    }
}

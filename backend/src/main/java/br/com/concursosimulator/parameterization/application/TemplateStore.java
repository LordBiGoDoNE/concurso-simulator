package br.com.concursosimulator.parameterization.application;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.questionbank.domain.QuestionContent;
import java.util.*;
import java.util.function.Function;

public interface TemplateStore {
    record BaseRevision(UUID questionId,UUID revisionId,String fingerprint,QuestionContent content,boolean eligible) {
        public BaseRevision {
            Objects.requireNonNull(questionId);Objects.requireNonNull(revisionId);Objects.requireNonNull(content);
            if(!content.fingerprint().equals(fingerprint))throw new IllegalArgumentException("Fingerprint da base divergente");
        }
    }
    interface Session {
        Optional<TemplateFamily> loadForUpdate(UUID id);
        Optional<TemplateFamily> findBaseForUpdate(UUID questionId);
        Optional<BaseRevision> base(UUID questionId,UUID revisionId);
        void insert(TemplateFamily family);
        void save(TemplateFamily family);
    }
    <T> T transaction(Function<Session,T> action);
    final class ConcurrentRegistration extends RuntimeException {public ConcurrentRegistration(Throwable cause){super(cause);}}
    final class Stale extends RuntimeException {}
}

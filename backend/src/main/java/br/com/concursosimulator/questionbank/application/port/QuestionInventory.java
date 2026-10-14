package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.domain.*;
import java.util.List;
import java.util.UUID;

/** Snapshot editorial privado, sem travas de escrita nem entidades de persistência. */
public interface QuestionInventory {
    List<Entry> read();
    record Entry(UUID questionId, QuestionOrigin origin, UUID eligibleRevisionId, int editorialSequence,
                 List<Revision> revisions) {
        public Entry { revisions = List.copyOf(revisions); }
        public Revision latest() { return revisions.getLast(); }
    }
    record Revision(UUID id, int number, String fingerprint, Question.Status status, QuestionClassification classification) {}
}

package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries.Criteria;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

/** Leitura administrativa privada do acervo, sem travas de escrita nem tipos de persistência. */
public interface AdministrativeQuestionCatalog {
    record Summary(UUID questionId, QuestionOrigin origin, int editorialSequence, LatestRevision latest,
                   EligibleRevision eligible, Instant lastActivityAt) {}
    record LatestRevision(UUID id, int number, Question.Status status, String fingerprint, String disciplineId, String topicId) {}
    /** reviewKind nulo: aprovação histórica sem origem informada. */
    record EligibleRevision(UUID id, String fingerprint, EditorialDecision.ReviewKind reviewKind) {}
    record Page(List<Summary> items, long total) { public Page { items = List.copyOf(items); } }
    record ImportLink(UUID receiptId, UUID actor, String destination, Instant committedAt, UUID revisionId, boolean reused) {}
    record ImportedRevision(UUID questionId, UUID revisionId) {}
    record StoredFile(String path, String mediaType, String sha256, byte[] bytes) {
        public StoredFile { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
        @Override public String toString() { return "StoredFile[private]"; }
    }

    Page search(Criteria criteria);
    /** Agregado completo (revisões, estados, decisões) lido sem trava, validado como na escrita. */
    Optional<Question> question(UUID questionId);
    List<ImportLink> imports(UUID questionId);
    /** Revisões registradas por um recibo de importação; vazio quando o recibo não existe. */
    Optional<List<ImportedRevision>> receipt(UUID receiptId);
    /** Evidência privada armazenada pela importação administrativa para o caminho e hash exatos. */
    Optional<StoredFile> file(String path, String sha256);
}

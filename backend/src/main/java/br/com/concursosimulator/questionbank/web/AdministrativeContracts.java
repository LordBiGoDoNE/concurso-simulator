package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries;
import br.com.concursosimulator.questionbank.application.ApplyQuestionCandidatesUseCase;
import br.com.concursosimulator.questionbank.application.BatchConfirmation;
import br.com.concursosimulator.questionbank.application.EditorialBatchUseCase;
import br.com.concursosimulator.questionbank.application.ImportConfirmation;
import br.com.concursosimulator.questionbank.application.ImportReceipt;
import br.com.concursosimulator.questionbank.application.port.AdministrativeQuestionCatalog;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import br.com.concursosimulator.questionbank.domain.Provenance;
import br.com.concursosimulator.questionbank.domain.Question;
import br.com.concursosimulator.questionbank.domain.QuestionContent;
import br.com.concursosimulator.questionbank.domain.QuestionOrigin;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import br.com.concursosimulator.questionbank.domain.StudyReference;
import br.com.concursosimulator.questionbank.domain.TextContent;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Contrato JSON da administração de questões (importação, curadoria e decisões em lote), separado dos tipos
 * do núcleo. A ordem e os nomes dos componentes são o contrato público: renomear algo no núcleo não muda o
 * JSON sem alterar estes mapeadores. {@code JsonContractSnapshot} prova a equivalência com o contrato anterior.
 */
public final class AdministrativeContracts {
    private AdministrativeContracts() {}

    public enum Purpose {
        SIMULADO,
        PRATICA_AULA
    }

    public enum Status {
        DRAFT,
        IN_REVIEW,
        APPROVED
    }

    public enum Action {
        SUBMIT,
        RETURN_TO_DRAFT,
        APPROVE,
        WITHDRAW
    }

    public enum Check {
        CORRECTNESS,
        DISTRACTORS,
        CLARITY,
        STEP_BY_STEP,
        ACCESSIBILITY,
        SOURCES,
        CLASSIFICATION
    }

    public enum ReviewKind {
        HUMAN_PEER,
        AI_ONLY
    }

    // Pacote de questões (entrada da importação e saída da exportação).
    public record Origin(String namespace, String key) {}

    public record Resource(String path, String mediaType, String sha256, String description) {}

    public record Text(String text, List<Resource> resources) {}

    public record Alternative(UUID id, Text content) {}

    public record LegalBasis(String provision, LocalDate checkedOn) {}

    public record Reference(String name, URI url, LegalBasis legalBasis) {}

    public record Source(
            String author, String contentPath, String answerPath, String contentSha256, String answerSha256) {}

    public record PackageClassification(String disciplineId, String topicId, List<String> roleIds) {}

    public record PackageContent(
            Text statement,
            List<Alternative> alternatives,
            UUID correctAlternativeId,
            Text resolution,
            PackageClassification classification,
            List<Reference> references,
            Source provenance) {}

    public record PackageItem(Origin origin, PackageContent content, String fingerprint, UUID expectedLatestRevision) {}

    public record PackageFile(String path, String mediaType, String sha256, String base64) {}

    public record PackageDocument(String format, Purpose purpose, List<PackageItem> items, List<PackageFile> files) {}

    // Importação.
    public record ImportConfirmationView(
            UUID nonce,
            UUID actor,
            String destination,
            String packageDigest,
            String selectionDigest,
            Instant issuedAt,
            Instant expiresAt) {}

    public record ImportResult(Origin origin, UUID questionId, UUID revisionId, boolean reused) {}

    public record ImportReceiptView(
            ImportConfirmationView confirmation, Instant committedAt, List<ImportResult> results) {}

    // Curadoria.
    public record LatestRevision(
            UUID id, int number, Status status, String fingerprint, String disciplineId, String topicId) {}

    public record EligibleRevision(UUID id, String fingerprint, ReviewKind reviewKind) {}

    public record ImportLink(
            UUID receiptId, UUID actor, String destination, Instant committedAt, UUID revisionId, boolean reused) {}

    public record LookupResult(
            Origin origin,
            String fingerprint,
            String match,
            UUID questionId,
            UUID latestRevisionId,
            String latestFingerprint,
            Status latestStatus,
            UUID eligibleRevisionId,
            String eligibleFingerprint,
            Integer editorialSequence,
            UUID matchingRevisionId) {}

    // Decisões em lote.
    public record Target(UUID questionId, UUID revisionId, String fingerprint) {}

    public record Expected(
            UUID questionId, UUID revisionId, String fingerprint, UUID eligibleRevisionId, Integer editorialSequence) {}

    public record BatchItem(
            UUID questionId,
            Origin origin,
            UUID revisionId,
            String fingerprint,
            Status currentStatus,
            Status resultingStatus,
            UUID eligibleRevisionId,
            Integer editorialSequence,
            String refusal) {}

    public record BatchConfirmationView(
            UUID nonce, UUID actor, String destination, String digest, Instant issuedAt, Instant expiresAt) {}

    public record AppliedDecision(
            UUID questionId, UUID revisionId, String fingerprint, int decisionSequence, Status resultingStatus) {}

    public record BatchReceipt(
            BatchConfirmationView confirmation,
            Instant committedAt,
            Action action,
            ReviewKind reviewKind,
            String responsible,
            List<AppliedDecision> decisions) {}

    // Núcleo → web.
    static <A extends Enum<A>, B extends Enum<B>> B map(A value, Class<B> type) {
        return value == null ? null : Enum.valueOf(type, value.name());
    }

    static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    static <A, B> List<B> list(List<A> values, Function<A, B> mapper) {
        return values == null ? null : values.stream().map(mapper).toList();
    }

    static Origin origin(QuestionOrigin o) {
        return o == null ? null : new Origin(o.namespace(), o.key());
    }

    static Text text(TextContent t) {
        return t == null
                ? null
                : new Text(
                        t.text(),
                        list(t.resources(), r -> new Resource(r.path(), r.mediaType(), r.sha256(), r.description())));
    }

    static Reference reference(StudyReference r) {
        return new Reference(
                r.name(),
                r.url(),
                r.legalBasis() == null
                        ? null
                        : new LegalBasis(
                                r.legalBasis().provision(), r.legalBasis().checkedOn()));
    }

    static Source source(Provenance p) {
        return p == null
                ? null
                : new Source(p.author(), p.contentPath(), p.answerPath(), p.contentSha256(), p.answerSha256());
    }

    static PackageContent content(QuestionImportReader.Content c) {
        if (c == null) return null;
        var k = c.classification();
        return new PackageContent(
                text(c.statement()),
                list(c.alternatives(), a -> new Alternative(a.id(), text(a.content()))),
                c.correctAlternativeId(),
                text(c.resolution()),
                k == null ? null : new PackageClassification(k.disciplineId(), k.topicId(), k.roleIds()),
                list(c.references(), AdministrativeContracts::reference),
                source(c.provenance()));
    }

    static PackageDocument document(QuestionImportReader.Document d) {
        return new PackageDocument(
                d.format(),
                map(d.purpose(), Purpose.class),
                list(
                        d.items(),
                        i -> new PackageItem(
                                origin(i.origin()), content(i.content()), i.fingerprint(), i.expectedLatestRevision())),
                list(d.files(), f -> new PackageFile(f.path(), f.mediaType(), f.sha256(), f.base64())));
    }

    static ImportReceiptView receipt(ImportReceipt r) {
        return new ImportReceiptView(
                confirmation(r.confirmation()), r.committedAt(), list(r.results(), AdministrativeContracts::result));
    }

    static ImportConfirmationView confirmation(ImportConfirmation c) {
        return new ImportConfirmationView(
                c.nonce(),
                c.actor(),
                c.destination(),
                c.packageDigest(),
                c.selectionDigest(),
                c.issuedAt(),
                c.expiresAt());
    }

    static ImportResult result(ApplyQuestionCandidatesUseCase.Result r) {
        return new ImportResult(origin(r.origin()), r.questionId(), r.revisionId(), r.reused());
    }

    static LatestRevision latest(AdministrativeQuestionCatalog.LatestRevision l) {
        return l == null
                ? null
                : new LatestRevision(
                        l.id(),
                        l.number(),
                        map(l.status(), Status.class),
                        l.fingerprint(),
                        l.disciplineId(),
                        l.topicId());
    }

    static EligibleRevision eligible(AdministrativeQuestionCatalog.EligibleRevision e) {
        return e == null ? null : new EligibleRevision(e.id(), e.fingerprint(), map(e.reviewKind(), ReviewKind.class));
    }

    static ImportLink link(AdministrativeQuestionCatalog.ImportLink l) {
        return new ImportLink(l.receiptId(), l.actor(), l.destination(), l.committedAt(), l.revisionId(), l.reused());
    }

    static LookupResult lookup(AdministrativeQuestionQueries.LookupResult r) {
        return new LookupResult(
                origin(r.origin()),
                r.fingerprint(),
                name(r.match()),
                r.questionId(),
                r.latestRevisionId(),
                r.latestFingerprint(),
                map(r.latestStatus(), Status.class),
                r.eligibleRevisionId(),
                r.eligibleFingerprint(),
                r.editorialSequence(),
                r.matchingRevisionId());
    }

    static BatchItem batchItem(EditorialBatchUseCase.PreviewItem i) {
        return new BatchItem(
                i.questionId(),
                origin(i.origin()),
                i.revisionId(),
                i.fingerprint(),
                map(i.currentStatus(), Status.class),
                map(i.resultingStatus(), Status.class),
                i.eligibleRevisionId(),
                i.editorialSequence(),
                name(i.refusal()));
    }

    static Expected expected(EditorialBatchUseCase.Expected e) {
        return new Expected(
                e.questionId(), e.revisionId(), e.fingerprint(), e.eligibleRevisionId(), e.editorialSequence());
    }

    static BatchReceipt batchReceipt(EditorialBatchUseCase.Receipt r) {
        var c = r.confirmation();
        return new BatchReceipt(
                batchConfirmation(c),
                r.committedAt(),
                map(r.action(), Action.class),
                map(r.reviewKind(), ReviewKind.class),
                r.responsible(),
                list(
                        r.decisions(),
                        d -> new AppliedDecision(
                                d.questionId(),
                                d.revisionId(),
                                d.fingerprint(),
                                d.decisionSequence(),
                                map(d.resultingStatus(), Status.class))));
    }

    static BatchConfirmationView batchConfirmation(BatchConfirmation c) {
        return new BatchConfirmationView(
                c.nonce(), c.actor(), c.destination(), c.digest(), c.issuedAt(), c.expiresAt());
    }

    static Status status(Question.Status s) {
        return map(s, Status.class);
    }

    // Web → núcleo. Recusas dos objetos de valor viram erro de entrada no ponto de chamada.
    static QuestionOrigin origin(Origin o) {
        return o == null ? null : new QuestionOrigin(o.namespace(), o.key());
    }

    static TextContent text(Text t) {
        return t == null
                ? null
                : new TextContent(
                        t.text(),
                        list(
                                t.resources(),
                                r -> new TextContent.Resource(r.path(), r.mediaType(), r.sha256(), r.description())));
    }

    static StudyReference reference(Reference r) {
        return r == null
                ? null
                : new StudyReference(
                        r.name(),
                        r.url(),
                        r.legalBasis() == null
                                ? null
                                : new StudyReference.LegalBasis(
                                        r.legalBasis().provision(),
                                        r.legalBasis().checkedOn()));
    }

    static QuestionImportReader.Content content(PackageContent c) {
        if (c == null) return null;
        var k = c.classification();
        return new QuestionImportReader.Content(
                text(c.statement()),
                list(
                        c.alternatives(),
                        a -> a == null ? null : new QuestionContent.Alternative(a.id(), text(a.content()))),
                c.correctAlternativeId(),
                text(c.resolution()),
                k == null ? null : new QuestionImportReader.Classification(k.disciplineId(), k.topicId(), k.roleIds()),
                list(c.references(), AdministrativeContracts::reference),
                c.provenance() == null
                        ? null
                        : new Provenance(
                                c.provenance().author(),
                                c.provenance().contentPath(),
                                c.provenance().answerPath(),
                                c.provenance().contentSha256(),
                                c.provenance().answerSha256()));
    }

    static QuestionImportReader.Document document(PackageDocument d) {
        if (d == null) return null;
        return new QuestionImportReader.Document(
                d.format(),
                map(d.purpose(), QuestionPurpose.class),
                list(
                        d.items(),
                        i -> i == null
                                ? null
                                : new QuestionImportReader.Item(
                                        origin(i.origin()),
                                        content(i.content()),
                                        i.fingerprint(),
                                        i.expectedLatestRevision())),
                list(
                        d.files(),
                        f -> f == null
                                ? null
                                : new QuestionImportReader.File(f.path(), f.mediaType(), f.sha256(), f.base64())));
    }

    static QuestionPurpose purpose(Purpose p) {
        return map(p, QuestionPurpose.class);
    }

    static Question.Status status(Status s) {
        return map(s, Question.Status.class);
    }

    static EditorialDecision.ReviewKind reviewKind(ReviewKind k) {
        return map(k, EditorialDecision.ReviewKind.class);
    }

    static EditorialDecision.Action action(Action a) {
        return map(a, EditorialDecision.Action.class);
    }

    static List<EditorialDecision.Check> checks(List<Check> checks) {
        return list(checks, c -> map(c, EditorialDecision.Check.class));
    }

    static List<EditorialBatchUseCase.Target> targets(List<Target> targets) {
        return list(
                targets,
                t -> t == null
                        ? null
                        : new EditorialBatchUseCase.Target(t.questionId(), t.revisionId(), t.fingerprint()));
    }

    static List<EditorialBatchUseCase.Expected> expected(List<Expected> expected) {
        return list(
                expected,
                e -> e == null
                        ? null
                        : new EditorialBatchUseCase.Expected(
                                e.questionId(),
                                e.revisionId(),
                                e.fingerprint(),
                                e.eligibleRevisionId(),
                                e.editorialSequence()));
    }
}

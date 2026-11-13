package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

/**
 * Consultas administrativas somente leitura (#177): busca, detalhe, lookup de pacote local e
 * exportação reimportável. Não escreve, não reserva identidades e não decide curadoria.
 */
public final class AdministrativeQuestionQueries {
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_LOOKUP = 1000;
    public static final int MAX_EXPORT = 100;
    private final AdministrativeQuestionCatalog catalog;
    private final QuestionInventory inventory;
    private final QuestionImportReader reader;

    public AdministrativeQuestionQueries(AdministrativeQuestionCatalog catalog, QuestionInventory inventory, QuestionImportReader reader) {
        this.catalog = Objects.requireNonNull(catalog); this.inventory = Objects.requireNonNull(inventory); this.reader = Objects.requireNonNull(reader);
    }

    /** Pedido fora dos limites/valores do contrato; não revela nada do acervo. */
    public static final class InvalidQuery extends RuntimeException {
        public InvalidQuery() { super("INVALID_QUERY"); }
    }

    /** Filtros combinados por AND; nulos não filtram. Atividade = última decisão ou importação registrada. */
    public record Criteria(QuestionPurpose purpose, String namespace, String keyPrefix, String disciplineId, String topicId,
                           Question.Status latestStatus, Boolean hasEligible, EditorialDecision.ReviewKind eligibleReviewKind,
                           Instant activityFrom, Instant activityTo, int page, int size) {
        public Criteria {
            require(page >= 0 && page <= 100_000 && size >= 1 && size <= MAX_PAGE_SIZE);
            require(namespace == null || !namespace.isBlank() && namespace.length() <= 100);
            require(keyPrefix == null || !keyPrefix.isEmpty() && keyPrefix.length() <= 200);
            try {
                if (disciplineId != null) Discipline.fromId(disciplineId);
                if (topicId != null) Topic.fromId(topicId);
            } catch (IllegalArgumentException invalid) { throw new InvalidQuery(); }
            require(activityFrom == null || activityTo == null || !activityFrom.isAfter(activityTo));
        }
    }

    public AdministrativeQuestionCatalog.Page search(Criteria criteria) {
        return catalog.search(Objects.requireNonNull(criteria));
    }

    public record Detail(Question question, List<AdministrativeQuestionCatalog.ImportLink> imports) {}

    public Optional<Detail> detail(UUID questionId) {
        return catalog.question(Objects.requireNonNull(questionId)).map(q -> new Detail(q, catalog.imports(q.id())));
    }

    public record LookupKey(QuestionOrigin origin, String fingerprint) {}
    /** PRESENT: existe e nenhum fingerprint foi informado. DIFFERENT pode indicar revisão anterior em matchingRevisionId. */
    public enum Match { ABSENT, PRESENT, SAME_AS_LATEST, SAME_AS_ELIGIBLE, DIFFERENT }
    public record LookupResult(QuestionOrigin origin, String fingerprint, Match match, UUID questionId,
                               UUID latestRevisionId, String latestFingerprint, Question.Status latestStatus,
                               UUID eligibleRevisionId, String eligibleFingerprint, Integer editorialSequence, UUID matchingRevisionId) {}

    public List<LookupResult> lookup(List<LookupKey> keys) {
        require(keys != null && !keys.isEmpty() && keys.size() <= MAX_LOOKUP);
        var origins = new LinkedHashSet<QuestionOrigin>();
        for (var key : keys) {
            require(key != null && key.origin() != null && origins.add(key.origin()));
            require(key.fingerprint() == null || key.fingerprint().matches("[0-9a-f]{64}"));
        }
        var found = new HashMap<QuestionOrigin, QuestionInventory.Entry>();
        for (var entry : inventory.read(origins)) found.put(entry.origin(), entry);
        var results = new ArrayList<LookupResult>();
        for (var key : keys) {
            var entry = found.get(key.origin());
            if (entry == null) {
                results.add(new LookupResult(key.origin(), key.fingerprint(), Match.ABSENT, null, null, null, null, null, null, null, null));
                continue;
            }
            var latest = entry.latest();
            var eligible = entry.revisions().stream().filter(r -> r.id().equals(entry.eligibleRevisionId())).findFirst();
            var matching = key.fingerprint() == null ? null
                    : entry.revisions().stream().filter(r -> r.fingerprint().equals(key.fingerprint())).map(QuestionInventory.Revision::id).findFirst().orElse(null);
            Match match;
            if (key.fingerprint() == null) match = Match.PRESENT;
            else if (latest.fingerprint().equals(key.fingerprint())) match = Match.SAME_AS_LATEST;
            else if (eligible.isPresent() && eligible.get().fingerprint().equals(key.fingerprint())) match = Match.SAME_AS_ELIGIBLE;
            else match = Match.DIFFERENT;
            results.add(new LookupResult(key.origin(), key.fingerprint(), match, entry.questionId(), latest.id(), latest.fingerprint(), latest.status(),
                    eligible.map(QuestionInventory.Revision::id).orElse(null), eligible.map(QuestionInventory.Revision::fingerprint).orElse(null),
                    entry.editorialSequence(), matching));
        }
        return List.copyOf(results);
    }

    /** Exatamente uma forma de seleção: questões (última revisão de cada) ou recibo de importação (revisões do recibo). */
    public record ExportSelection(List<UUID> questionIds, UUID importReceiptId) {}
    public enum ExportFailure { NOT_FOUND, MIXED_PURPOSE, ORIGIN_NOT_EXPORTABLE, MISSING_EVIDENCE, LIMIT_EXCEEDED }
    public static final class ExportRefused extends RuntimeException {
        private final ExportFailure failure; private final List<UUID> questionIds;
        public ExportRefused(ExportFailure failure, Collection<UUID> questionIds) {
            super("EXPORT_REFUSED"); this.failure = failure; this.questionIds = questionIds.stream().sorted().toList();
        }
        public ExportFailure failure() { return failure; }
        public List<UUID> questionIds() { return questionIds; }
    }
    public record Export(QuestionImportReader.Document document, List<String> keys, String packageDigest, String selectionDigest) {}

    public Export export(ExportSelection selection) {
        require(selection != null && (selection.questionIds() == null) != (selection.importReceiptId() == null));
        var revisions = new LinkedHashMap<UUID, UUID>(); // questionId -> revisionId (null = última revisão)
        if (selection.questionIds() != null) {
            require(!selection.questionIds().isEmpty() && selection.questionIds().size() <= MAX_EXPORT);
            for (var id : selection.questionIds()) require(id != null && !revisions.containsKey(id) && revisions.put(id, null) == null);
        } else {
            var imported = catalog.receipt(selection.importReceiptId()).orElseThrow(() -> new ExportRefused(ExportFailure.NOT_FOUND, List.of()));
            for (var item : imported) revisions.put(item.questionId(), item.revisionId());
            if (revisions.isEmpty() || revisions.size() > MAX_EXPORT) throw new ExportRefused(ExportFailure.LIMIT_EXCEEDED, List.of());
        }
        var missing = new ArrayList<UUID>(); var exported = new ArrayList<Map.Entry<Question, QuestionRevision>>();
        for (var entry : revisions.entrySet()) {
            var question = catalog.question(entry.getKey());
            if (question.isEmpty()) { missing.add(entry.getKey()); continue; }
            var q = question.get();
            exported.add(Map.entry(q, entry.getValue() == null ? q.latest() : q.revision(entry.getValue())));
        }
        if (!missing.isEmpty()) throw new ExportRefused(ExportFailure.NOT_FOUND, missing);
        var purposes = exported.stream().map(e -> e.getKey().origin().purpose()).distinct().toList();
        if (purposes.size() != 1) throw new ExportRefused(ExportFailure.MIXED_PURPOSE, revisions.keySet());
        var purpose = purposes.getFirst();
        var invalidOrigins = exported.stream().map(Map.Entry::getKey).filter(q -> !exportable(q.origin(), purpose)).map(Question::id).toList();
        if (!invalidOrigins.isEmpty()) throw new ExportRefused(ExportFailure.ORIGIN_NOT_EXPORTABLE, invalidOrigins);
        var files = new TreeMap<String, QuestionImportReader.File>(); var withoutEvidence = new LinkedHashSet<UUID>();
        var items = new ArrayList<QuestionImportReader.Item>();
        for (var entry : exported) {
            var content = entry.getValue().content();
            for (var needed : evidence(content)) {
                var known = files.get(needed.getKey());
                if (known != null) {
                    if (!known.sha256().equals(needed.getValue())) withoutEvidence.add(entry.getKey().id());
                    continue;
                }
                var stored = catalog.file(needed.getKey(), needed.getValue());
                if (stored.isEmpty()) { withoutEvidence.add(entry.getKey().id()); continue; }
                var file = stored.get();
                files.put(file.path(), new QuestionImportReader.File(file.path(), file.mediaType(), file.sha256(), Base64.getEncoder().encodeToString(file.bytes())));
            }
            items.add(new QuestionImportReader.Item(entry.getKey().origin(), packageContent(content), entry.getValue().fingerprint(), null));
        }
        if (!withoutEvidence.isEmpty()) throw new ExportRefused(ExportFailure.MISSING_EVIDENCE, withoutEvidence);
        items.sort(Comparator.comparing(i -> i.origin().key()));
        var document = new QuestionImportReader.Document("administrative-question-package/v1", purpose, List.copyOf(items), List.copyOf(files.values()));
        var keys = items.stream().map(i -> i.origin().key()).toList();
        try {
            // O próprio leitor de importação confirma que o pacote exportado é reimportável.
            var pack = reader.read(document, keys);
            return new Export(document, keys, pack.digest(), pack.selectionDigest());
        } catch (QuestionImportReader.InvalidPackage invalid) {
            throw new ExportRefused(ExportFailure.LIMIT_EXCEEDED, revisions.keySet());
        }
    }

    /** Mesmo formato aberto de conteúdo do pacote de importação (classificação por IDs). */
    public static QuestionImportReader.Content packageContent(QuestionContent c) {
        var classification = new QuestionImportReader.Classification(c.classification().discipline().id(), c.classification().topic().id(),
                c.classification().roles().stream().map(TargetRole::id).sorted().toList());
        return new QuestionImportReader.Content(c.statement(), c.alternatives(), c.correctAlternativeId(), c.resolution(), classification, c.references(), c.provenance());
    }

    private static boolean exportable(QuestionOrigin origin, QuestionPurpose purpose) {
        if (!origin.key().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")) return false;
        return purpose == QuestionPurpose.SIMULADO ? origin.namespace().equals("admin-question-v1")
                : origin.namespace().equals(QuestionPurpose.PRACTICE_NAMESPACE) && origin.key().startsWith("admin.") && origin.key().length() > 6;
    }

    private static List<Map.Entry<String, String>> evidence(QuestionContent content) {
        var needed = new ArrayList<Map.Entry<String, String>>();
        needed.add(Map.entry(content.provenance().contentPath(), content.provenance().contentSha256()));
        needed.add(Map.entry(content.provenance().answerPath(), content.provenance().answerSha256()));
        var blocks = new ArrayList<TextContent>(); blocks.add(content.statement()); blocks.add(content.resolution());
        content.alternatives().forEach(a -> blocks.add(a.content()));
        for (var block : blocks) for (var resource : block.resources()) needed.add(Map.entry(resource.path(), resource.sha256()));
        return needed;
    }

    private static void require(boolean valid) { if (!valid) throw new InvalidQuery(); }
}

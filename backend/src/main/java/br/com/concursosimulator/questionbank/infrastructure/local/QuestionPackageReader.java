package br.com.concursosimulator.questionbank.infrastructure.local;

import br.com.concursosimulator.questionbank.application.ApplyQuestionCandidatesUseCase.Candidate;
import br.com.concursosimulator.questionbank.domain.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import java.net.URI;
import java.util.*;

/** Entrada JSON estrita; seleção explícita inteira, hashes de fonte e fingerprint recalculados. */
public final class QuestionPackageReader {
    private final JsonMapper json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    public record CatalogItem(String topicId, String disciplineId) {}
    public record SourceFile(String path, String sha256) {}
    public record InventoryItem(String key, String status, String legalBasisPending, boolean editorialReviewRequired) {}
    public record Issue(String key, String message) {}
    public record Classification(String disciplineId, String topicId, List<String> roleIds) {}
    public record Content(TextContent statement, List<QuestionContent.Alternative> alternatives, UUID correctAlternativeId,
                          TextContent resolution, Classification classification, List<StudyReference> references, Provenance provenance) {
        QuestionContent domain() { return new QuestionContent(statement, alternatives, correctAlternativeId, resolution,
                QuestionClassification.fromIds(classification.disciplineId(), classification.topicId(), classification.roleIds()), references, provenance); }
    }
    public record Item(QuestionOrigin origin, Content content, String fingerprint, UUID expectedLatestRevision) {}
    public record Package(String format, List<CatalogItem> catalog, List<SourceFile> sourceFiles, List<InventoryItem> inventory,
                          List<Item> items, List<Issue> issues, List<List<String>> possibleDuplicates) {}
    public record Selected(List<Candidate> candidates, List<InventoryItem> inventory, List<Issue> issues, List<List<String>> possibleDuplicates) {}
    public Selected read(Path file, Path root, Set<String> keys) throws java.io.IOException {
        return read(LocalFiles.bounded(file, 32_000_000), root, keys);
    }
    public Selected read(byte[] bytes, Path root, Set<String> keys) throws java.io.IOException {
        if (keys.isEmpty()) throw new IllegalArgumentException("Seleção de chaves obrigatória");
        var pack = json.readValue(bytes, Package.class);
        if (!"question-package/v1".equals(pack.format())) throw new IllegalArgumentException("Formato não suportado");
        var catalog = new HashMap<String, String>();
        for (var item : pack.catalog()) if (catalog.put(item.topicId(), item.disciplineId()) != null) throw new IllegalArgumentException("Catálogo repetido");
        if (catalog.size() != Topic.values().length) throw new IllegalArgumentException("Catálogo incompleto");
        for (var topic : Topic.values()) if (!topic.discipline().id().equals(catalog.get(topic.id()))) throw new IllegalArgumentException("Catálogo divergente");
        var sources = new HashMap<String, String>();
        for (var source : pack.sourceFiles()) {
            if (sources.put(source.path(), source.sha256()) != null || !LocalFiles.sha(LocalFiles.source(root, source.path())).equals(source.sha256()))
                throw new IllegalArgumentException("Fonte repetida ou hash divergente");
        }
        if (!sources.containsKey("manifesto_v3.json") || !sources.containsKey("catalogo_fontes_questoes.csv")) throw new IllegalArgumentException("Snapshot incompleto");
        var inventory = new HashMap<String, InventoryItem>();
        for (var row : pack.inventory()) {
            if (inventory.put(row.key(), row) != null || !row.editorialReviewRequired() || !Set.of("candidate", "invalid").contains(row.status()))
                throw new IllegalArgumentException("Inventário inválido");
        }
        if (inventory.size() != 510) throw new IllegalArgumentException("Inventário incompleto");
        var unknown=keys.stream().filter(key -> !inventory.containsKey(key)).map(key -> new LocalPackageDiagnostics.Diagnostic(key,"UNKNOWN_SELECTED_KEY")).toList();
        if (!unknown.isEmpty()) throw new LocalPackageDiagnostics.InvalidSelection(unknown);
        for (var topic : Topic.values()) for (int number=1; number<=10; number++)
            if (!inventory.containsKey(topic.id() + "-Q" + String.format(java.util.Locale.ROOT, "%02d", number))) throw new IllegalArgumentException("ID fora do inventário aprovado");
        var manifest = json.readTree(LocalFiles.source(root, "manifesto_v3.json"));
        var paths = new HashMap<String, List<String>>();
        for (var module : manifest.get("modulos")) paths.put(module.get("id").asString(), List.of(module.get("conteudo").asString(), module.get("gabarito").asString()));
        var items = new HashMap<String, Item>();
        for (var item : pack.items()) {
            if (!"legacy-v3".equals(item.origin().namespace()) || items.put(item.origin().key(), item) != null
                    || !inventory.containsKey(item.origin().key()) || !"candidate".equals(inventory.get(item.origin().key()).status()))
                throw new IllegalArgumentException("Origem/ID repetido ou fora do inventário");
        }
        var selected = new ArrayList<Candidate>();
        var invalid = keys.stream().sorted().filter(key -> !items.containsKey(key) || !"candidate".equals(inventory.get(key).status())
                || pack.issues().stream().anyMatch(i -> key.equals(i.key()))).map(key -> new LocalPackageDiagnostics.Diagnostic(key,"INVALID_SELECTED_ITEM")).toList();
        if (!invalid.isEmpty()) throw new LocalPackageDiagnostics.InvalidSelection(invalid);
        for (var key : keys.stream().sorted().toList()) {
            var item = items.get(key);
            var content = item.content().domain(); var provenance = content.provenance();
            // Chave/proveniência identificam o módulo histórico, não a classificação da revisão nova.
            var originTopic = key.substring(0,key.indexOf("-Q"));
            if (!Objects.equals(sources.get(provenance.contentPath()), provenance.contentSha256())
                    || !Objects.equals(sources.get(provenance.answerPath()), provenance.answerSha256())) throw new IllegalArgumentException("Proveniência fora do snapshot");
            if (!List.of(provenance.contentPath(), provenance.answerPath()).equals(paths.get(originTopic)))
                throw new IllegalArgumentException("Join origem/caminhos divergente do manifesto");
            if (!content.fingerprint().equals(item.fingerprint())) throw new IllegalArgumentException("Fingerprint divergente: " + key);
            PackageContentSafety.check(root, content);
            selected.add(new Candidate(item.origin(), content, item.expectedLatestRevision()));
        }
        return new Selected(List.copyOf(selected), List.copyOf(pack.inventory()), List.copyOf(pack.issues()), List.copyOf(pack.possibleDuplicates()));
    }
}

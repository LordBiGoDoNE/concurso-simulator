package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.PreviewQuestionCandidatesUseCase;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionInventory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.util.*;

/** Ensaio deliberado do acervo externo: leitor e prévia somente, banco efêmero próprio. */
public final class QuestionExpansionContentCheck {
    private QuestionExpansionContentCheck() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].equals("--requests"))
            throw new IllegalArgumentException("Uso: --requests diretório-externo");
        var directory = Path.of(args[1]).toAbsolutePath().normalize();
        var json = JsonMapper.builder().build();
        var packages = new ArrayList<Map<String, Object>>();
        var files = new ArrayList<Path>();
        try (var walk = Files.walk(directory)) {
            walk.filter(p -> p.getFileName().toString().equals("preview-request.json"))
                    .sorted().forEach(files::add);
        }
        if (files.isEmpty()) throw new IllegalArgumentException("Nenhum pacote selecionado");
        int total = 0;
        try (var pg = new PostgreSQLContainer("postgres:18.6")) {
            pg.start();
            try (var db = new QuestionDatabase(pg)) {
                var inventory = new JpaQuestionInventory(SharedEntityManagerCreator.createSharedEntityManager(db.factory), db.manager);
                var preview = new PreviewQuestionCandidatesUseCase(inventory);
                for (var file : files) {
                    var request = json.readTree(Files.readAllBytes(file));
                    var document = json.treeToValue(request.get("package"), AdministrativePackageReader.Document.class);
                    var keys = request.get("keys").valueStream().map(n -> n.asString()).toList();
                    var pack = new AdministrativePackageReader().read(document, keys);
                    var expected = json.readTree(Files.readAllBytes(file.resolveSibling("digests.json")));
                    if (!pack.digest().equals(expected.path("packageDigest").asString())
                            || !pack.selectionDigest().equals(expected.path("selectionDigest").asString()))
                        throw new IllegalStateException("Digest divergente");
                    var result = preview.execute(pack.candidates());
                    if (result.size() != keys.size() || result.stream().anyMatch(r -> r.change() != PreviewQuestionCandidatesUseCase.Change.NEW))
                        throw new IllegalStateException("Prévia não corresponde ao destino novo isolado");
                    total += keys.size();
                    packages.add(Map.of("purpose", pack.purpose().name(), "items", keys.size(),
                            "packageDigest", pack.digest(), "selectionDigest", pack.selectionDigest()));
                }
                if (db.jdbc.queryForObject("select count(*) from qb_question", Integer.class) != 0
                        || db.jdbc.queryForObject("select count(*) from qb_import_receipt", Integer.class) != 0
                        || db.jdbc.queryForObject("select count(*) from qb_decision", Integer.class) != 0)
                    throw new IllegalStateException("Prévia provocou escrita");
            }
        }
        var report = Map.of("destination", "isolated-new-postgres-173-preview", "packages", packages,
                "items", total, "imported", false, "eligible", false,
                "limits", "Leitor/prévia em destino vazio; não comprova revisão editorial, ausência de conflitos em outro destino ou cobertura elegível.");
        Files.writeString(directory.resolve("reader-preview-report.json"), json.writeValueAsString(report)+"\n");
        System.out.println("Pacotes reais validados pelo leitor e prévia: "+packages.size()+"; itens: "+total+"; sem importação/aprovação.");
    }
}

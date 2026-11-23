package br.com.concursosimulator.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.concursosimulator.questionbank.infrastructure.local.QuestionPackageReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Valida as 250 revisões reais sem importar, enviar à revisão ou aprovar acervo em CI. */
class CuratedPackageTests {
    @TempDir
    Path temp;

    @Test
    void allCuratedPackagesHaveMatchingJavaFingerprintsAndNoImplicitApproval() throws Exception {
        var root = Path.of("..").toAbsolutePath().normalize();
        var python = System.getProperty("os.name").startsWith("Windows") ? "python" : "python3";
        var process = new ProcessBuilder(
                        python, "-B", "scripts/curate_questions.py", "--root", ".", "--output", temp.toString())
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        var diagnostic = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).withFailMessage(diagnostic).isZero();
        var json = JsonMapper.builder().build();
        var ids = new HashSet<String>();
        // Os pacotes 2026-10 fixam hashes das cópias congeladas (D8), não das aulas vivas.
        var sources = root.resolve("editorial/question-bank/2026-10/legacy-sources");
        var reader = new QuestionPackageReader();
        try (var paths = Files.list(temp.resolve("packages"))) {
            var packages = paths.sorted().toList();
            assertThat(packages).hasSize(25);
            for (var file : packages) {
                var tree = json.readTree(Files.readAllBytes(file));
                var keys = new HashSet<String>();
                for (var item : tree.get("items"))
                    keys.add(item.get("origin").get("key").asString());
                assertThat(keys).hasSize(10);
                assertThat(keys).noneMatch(ids::contains);
                ids.addAll(keys);
                var selected = reader.read(file, sources, keys);
                assertThat(selected.inventory()).hasSize(510);
                assertThat(selected.candidates()).hasSize(10).allSatisfy(candidate -> {
                    assertThat(candidate.expectedLatestRevision()).isNull();
                    assertThat(candidate.content().resolution().text())
                            .startsWith("1. ")
                            .contains("2. ");
                    assertThat(candidate.content().references()).isNotEmpty();
                });
            }
        }
        assertThat(ids).hasSize(250).doesNotContain("P05-Q01");
        var selected = reader.read(temp.resolve("all-selected.json"), sources, ids);
        var disciplines = selected.candidates().stream()
                .collect(Collectors.groupingBy(
                        c -> c.content().classification().discipline().id(), Collectors.counting()));
        assertThat(disciplines)
                .hasSize(5)
                .allSatisfy((key, count) -> assertThat(count).isEqualTo(50));
        assertThat(selected.candidates().stream()
                        .map(c -> c.content().classification().topic())
                        .distinct())
                .hasSize(51);
        assertThatThrownBy(() -> reader.read(temp.resolve("all-selected.json"), sources, java.util.Set.of("P05-Q01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package br.com.concursosimulator.questionbank;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Snapshot do JSON público administrativo (backend-http-boundaries, grupo 5): nomes, aninhamento, tipos e
 * valores, com UUIDs, instantes e tokens normalizados. Gerado antes da troca para DTOs web próprios; a troca
 * não pode mudá-lo. Regenerar: {@code ADMIN_CONTRACT_UPDATE=true}.
 */
final class JsonContractSnapshot {
    private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern INSTANT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private JsonContractSnapshot() {}

    static void check(String name, JsonNode actual) throws Exception {
        var file = Path.of("src/test/resources/http/admin-contracts/" + name + ".json");
        var normalized = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(normalize(actual)) + "\n";
        if ("true".equals(System.getenv("ADMIN_CONTRACT_UPDATE"))) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, normalized, StandardCharsets.UTF_8);
            return;
        }
        assertThat(file).as("Gere o contrato com ADMIN_CONTRACT_UPDATE=true antes de trocar os DTOs").exists();
        assertThat(normalized).as("contrato JSON " + name).isEqualTo(Files.readString(file, StandardCharsets.UTF_8));
    }

    static JsonNode normalize(JsonNode node) {
        if (node instanceof ObjectNode object) {
            var copy = JSON.createObjectNode();
            for (var name : object.propertyNames()) {
                var value = object.get(name);
                if (value.isString() && name.toLowerCase().endsWith("token")) copy.set(name, StringNode.valueOf("<token>"));
                // Digest do lote cobre IDs aleatórios; packageDigest e selectionDigest são determinísticos e ficam.
                else if (value.isString() && name.equals("digest")) copy.set(name, StringNode.valueOf("<digest>"));
                else copy.set(name, normalize(value));
            }
            return copy;
        }
        if (node instanceof ArrayNode array) {
            var copy = JSON.createArrayNode();
            for (var value : array) copy.add(normalize(value));
            return copy;
        }
        if (node.isString()) {
            var text = UUID_TEXT.matcher(node.asString()).replaceAll("<uuid>");
            return StringNode.valueOf(INSTANT.matcher(text).replaceAll("<instant>"));
        }
        return node;
    }
}

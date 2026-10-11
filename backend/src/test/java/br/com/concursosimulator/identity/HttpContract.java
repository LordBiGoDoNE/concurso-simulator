package br.com.concursosimulator.identity;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;

final class HttpContract {
    private HttpContract() {}

    @SuppressWarnings("unchecked")
    static void json(String path, int status, HttpResponse<String> response) throws Exception {
        Map<String, Object> api = new Yaml().load(Files.readString(Path.of("openapi.yaml")));
        var operation = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) api.get("paths")).get(path)).get("get");
        var definition = (Map<String, Object>) ((Map<String, Object>) operation.get("responses")).get(Integer.toString(status));
        var media = (Map<String, Object>) ((Map<String, Object>) definition.get("content")).get("application/json");
        var schema = (Map<String, Object>) media.get("schema");
        Map<String, Object> body = new Yaml().load(response.body());
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(body.keySet()).containsExactlyInAnyOrderElementsOf((List<String>) schema.get("required"));
        var properties = (Map<String, Object>) schema.get("properties");
        for (var entry : body.entrySet()) {
            var property = (Map<String, Object>) properties.get(entry.getKey());
            if (property.get("type").equals("string")) assertThat(entry.getValue()).isInstanceOf(String.class);
            if (property.get("type").equals("boolean")) assertThat(entry.getValue()).isInstanceOf(Boolean.class);
            if (property.containsKey("const")) assertThat(entry.getValue()).isEqualTo(property.get("const"));
            if ("uuid".equals(property.get("format"))) UUID.fromString(entry.getValue().toString());
        }
    }
}

package br.com.concursosimulator.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verificador comum da forma de erro HTTP (design D5): objeto JSON com {@code error} em UPPER_SNAKE_CASE,
 * {@code Cache-Control: no-store} e nenhum eco de exceção, pilha ou SQL. Só os campos extras já existentes no
 * contrato são aceitos além de {@code error}, e somente quando o chamador os declara.
 */
public final class ErrorResponses {
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Set<String> KNOWN_EXTRAS = Set.of("correlationId", "available", "questionIds", "reason");
    /** Marcas de eco de exceção, pilha, SQL ou corpo de erro padrão do Spring. */
    private static final List<String> LEAKS = List.of(
            "Exception",
            "exception",
            "\tat ",
            "at br.com.",
            "at org.",
            "at java.",
            "SQLState",
            "SQL",
            "select ",
            "insert ",
            "update ",
            "delete ",
            "constraint",
            "PSQL",
            "org.hibernate",
            "org.springframework",
            "java.",
            "\"trace\"",
            "\"message\"",
            "\"timestamp\"",
            "\"path\"",
            "\"status\"");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ErrorResponses() {}

    /** Erro com status e código esperados e corpo só com {@code error}, salvo campos extras declarados. */
    public static void assertError(HttpResponse<String> response, int status, String code, String... extras) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(assertShape(response, extras)).isEqualTo(code);
    }

    /** Erro com o status esperado e forma válida, qualquer código. */
    public static void assertError(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertShape(response);
    }

    /** Confere a forma comum e devolve o código. */
    public static String assertShape(HttpResponse<String> response, String... extras) {
        assertThat(response.statusCode()).as("status de erro").isGreaterThanOrEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .as("Content-Type do erro")
                .startsWith("application/json");
        assertThat(response.headers().firstValue("Cache-Control").orElse(""))
                .as("Cache-Control do erro")
                .contains("no-store");
        var body = response.body();
        for (var leak : LEAKS)
            assertThat(body).as("corpo de erro sem eco interno").doesNotContain(leak);
        var tree = JSON.readTree(body);
        assertThat(tree.isObject()).as(body).isTrue();
        var error = tree.get("error");
        assertThat(error).as("campo error presente: " + body).isNotNull();
        assertThat(error.isString()).as(body).isTrue();
        assertThat(error.asString()).matches(CODE);
        var allowed = new HashSet<String>(List.of("error"));
        for (var extra : extras) {
            assertThat(KNOWN_EXTRAS).as("campo extra conhecido").contains(extra);
            allowed.add(extra);
        }
        var keys = new HashSet<String>();
        tree.propertyNames().forEach(keys::add);
        assertThat(allowed).as("campos do erro: " + body).containsAll(keys);
        return error.asString();
    }
}

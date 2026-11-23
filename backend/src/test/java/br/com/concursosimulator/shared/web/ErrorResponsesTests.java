package br.com.concursosimulator.shared.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

/** Controle negativo do verificador comum: ele precisa recusar cada desvio do contrato de erro. */
class ErrorResponsesTests {
    private static final Map<String, List<String>> JSON_NO_STORE =
            Map.of("Content-Type", List.of("application/json"), "Cache-Control", List.of("no-store"));

    @Test
    void acceptsTheContract() {
        assertThatCode(() -> ErrorResponses.assertError(
                        response(400, JSON_NO_STORE, "{\"error\":\"INVALID_REQUEST\"}"), 400, "INVALID_REQUEST"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ErrorResponses.assertError(
                        response(409, JSON_NO_STORE, "{\"error\":\"INSUFFICIENT_POOL\",\"available\":[]}"),
                        409,
                        "INSUFFICIENT_POOL",
                        "available"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEachDeviation() {
        for (var body : List.of(
                "{\"error\":\"invalid_request\"}",
                "{\"error\":\"INVALID-REQUEST\"}",
                "{}",
                "{\"error\":1}",
                "[]",
                "{\"error\":\"INTERNAL_ERROR\",\"message\":\"boom\"}",
                "{\"error\":\"INTERNAL_ERROR\",\"detail\":\"x\"}",
                "{\"error\":\"INTERNAL_ERROR\",\"x\":\"java.lang.IllegalStateException\"}",
                "{\"error\":\"INTERNAL_ERROR\",\"x\":\"ERROR: duplicate key value violates unique constraint\"}",
                "{\"error\":\"INTERNAL_ERROR\",\"available\":[]}")) {
            assertThatThrownBy(() -> ErrorResponses.assertError(response(500, JSON_NO_STORE, body), 500))
                    .as(body)
                    .isInstanceOf(AssertionError.class);
        }
        assertThatThrownBy(() -> ErrorResponses.assertError(
                        response(
                                400,
                                Map.of("Content-Type", List.of("application/json")),
                                "{\"error\":\"INVALID_REQUEST\"}"),
                        400))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> ErrorResponses.assertError(
                        response(400, Map.of("Cache-Control", List.of("no-store")), "{\"error\":\"INVALID_REQUEST\"}"),
                        400))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> ErrorResponses.assertError(response(200, JSON_NO_STORE, "{\"error\":\"OK\"}"), 200))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> ErrorResponses.assertError(
                        response(404, JSON_NO_STORE, "{\"error\":\"NOT_FOUND\"}"), 404, "CONFLICT"))
                .isInstanceOf(AssertionError.class);
    }

    private static HttpResponse<String> response(int status, Map<String, List<String>> headers, String body) {
        return new HttpResponse<>() {
            public int statusCode() {
                return status;
            }

            public HttpRequest request() {
                return HttpRequest.newBuilder(URI.create("http://127.0.0.1/")).build();
            }

            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            public HttpHeaders headers() {
                return HttpHeaders.of(headers, (name, value) -> true);
            }

            public String body() {
                return body;
            }

            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            public URI uri() {
                return URI.create("http://127.0.0.1/");
            }

            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}

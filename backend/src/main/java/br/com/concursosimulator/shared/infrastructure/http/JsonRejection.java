package br.com.concursosimulator.shared.infrastructure.http;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Rejeições escritas por filtros e handlers de segurança, antes do MVC, no mesmo formato da API. */
public final class JsonRejection {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]*");

    private JsonRejection() {}

    public static void write(HttpServletResponse response, int status, String code) throws IOException {
        body(response, status, "{\"error\":\"" + code(code) + "\"}");
    }

    /** Igual a {@link #write}, com um {@code correlationId} novo para o diário do servidor. */
    public static String writeCorrelated(HttpServletResponse response, int status, String code) throws IOException {
        var correlation = UUID.randomUUID().toString();
        body(response, status, "{\"error\":\"" + code(code) + "\",\"correlationId\":\"" + correlation + "\"}");
        return correlation;
    }

    private static void body(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json);
    }

    private static String code(String code) {
        if (code == null || !CODE.matcher(code).matches())
            throw new IllegalStateException("Código de erro fora do padrão: " + code);
        return code;
    }
}

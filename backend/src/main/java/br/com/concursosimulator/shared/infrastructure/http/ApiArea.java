package br.com.concursosimulator.shared.infrastructure.http;

import java.util.List;
import java.util.Objects;

/**
 * Área HTTP declarada pela funcionalidade dona: caminhos que a área possui, cadeia de segurança,
 * CORS e rotas com seu acesso técnico. Não decide identidade, posse ou regra de negócio.
 *
 * @param name identificador estável da área, usado em mensagens de diagnóstico
 * @param chain cadeia de segurança que atende os caminhos da área
 * @param paths padrões ({@code PathPattern}) dos caminhos possuídos pela área
 * @param cors política CORS aplicada a {@code corsPaths}; {@code null} quando a área não aceita origem cruzada
 * @param corsPaths padrões que recebem a política CORS
 * @param jsonFailures se 403 da segurança e indisponibilidade da sessão na área respondem JSON {@code no-store}
 * @param routes rotas atendidas, cada uma com método, padrão e acesso
 */
public record ApiArea(
        String name,
        Chain chain,
        List<String> paths,
        Cors cors,
        List<String> corsPaths,
        boolean jsonFailures,
        List<ApiRoute> routes) {
    public enum Chain {
        /** Sem sessão nem contexto de segurança; só leitura pública ou negação. */
        STATELESS,
        /** Cadeia padrão com sessão, CSRF e identidade verificada. */
        SESSION,
        /** Cadeia própria da funcionalidade (filtros de corpo/limite); usa o registro só para caminho e CORS. */
        OWN
    }

    /** Métodos aceitos em origem cruzada e se a origem pode enviar credenciais. */
    public record Cors(List<String> methods, boolean credentials) {
        public static Cors publicRead() {
            return new Cors(List.of("GET"), false);
        }

        public static Cors credentialed(String... methods) {
            return new Cors(List.of(methods), true);
        }
    }

    public ApiArea {
        Objects.requireNonNull(name);
        Objects.requireNonNull(chain);
        paths = List.copyOf(paths);
        corsPaths = List.copyOf(corsPaths);
        routes = List.copyOf(routes);
        if (paths.isEmpty()) throw new IllegalArgumentException("Área sem caminhos: " + name);
        if (cors == null != corsPaths.isEmpty()) throw new IllegalArgumentException("CORS incompleto na área " + name);
    }
}

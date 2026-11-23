package br.com.concursosimulator.shared.infrastructure.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.http.server.PathContainer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Registro único das áreas HTTP declaradas pelas funcionalidades. Cadeias de segurança, CORS, respostas
 * de rejeição, desvio de sessão e isenções leem daqui; nenhum outro ponto repete caminhos de rota.
 *
 * <p>Correspondência por {@code PathPattern} sobre {@code servletPath}, sem barra final opcional.
 * Falha na criação se duas declarações repetirem método e padrão, ou se uma rota ficar fora dos
 * caminhos da própria área.
 */
public final class ApiRoutes {
    /** Espaço administrativo: {@code no-store} e indisponibilidade comuns a todas as rotas sob ele, declaradas ou não. */
    public static final String ADMIN_NAMESPACE = "/api/v1/admin/";
    private record Compiled(ApiArea area, ApiRoute route, PathPattern pattern) {}
    private final List<ApiArea> areas;
    private final List<Compiled> routes = new ArrayList<>();
    private final List<PathPattern[]> owned = new ArrayList<>();

    public ApiRoutes(List<ApiArea> declared) {
        var parser = PathPatternParser.defaultInstance;
        areas = declared.stream().sorted(Comparator.comparing(ApiArea::name)).toList();
        var seen = new HashMap<String, String>();
        for (var area : areas) {
            if (seen.put("area " + area.name(), area.name()) != null) throw new IllegalStateException("Área HTTP duplicada: " + area.name());
            var paths = area.paths().stream().map(parser::parse).toArray(PathPattern[]::new);
            owned.add(paths);
            for (var route : area.routes()) {
                var key = route.method() + " " + route.pattern();
                var previous = seen.put(key, area.name());
                if (previous != null) throw new IllegalStateException("Rota HTTP declarada duas vezes: " + key + " (" + previous + ", " + area.name() + ")");
                var pattern = parser.parse(route.pattern());
                var concrete = PathContainer.parsePath(normalized(route.pattern(), "x"));
                if (java.util.Arrays.stream(paths).noneMatch(p -> p.matches(concrete)))
                    throw new IllegalStateException("Rota fora dos caminhos da área " + area.name() + ": " + key);
                routes.add(new Compiled(area, route, pattern));
            }
        }
    }

    public List<ApiArea> areas() { return areas; }

    /** Rota declarada mais específica para método e caminho, se houver. */
    public Optional<ApiRoute> route(HttpServletRequest request) { return compiled(request).map(Compiled::route); }

    /** Área que possui o caminho da requisição, qualquer que seja o método; o padrão mais específico vence. */
    public Optional<ApiArea> area(HttpServletRequest request) {
        var path = path(request);
        ApiArea best = null; PathPattern bestPattern = null;
        for (int i = 0; i < areas.size(); i++)
            for (var pattern : owned.get(i))
                if (pattern.matches(path) && (bestPattern == null || PathPattern.SPECIFICITY_COMPARATOR.compare(pattern, bestPattern) < 0)) {
                    best = areas.get(i); bestPattern = pattern;
                }
        return Optional.ofNullable(best);
    }

    public boolean owns(ApiArea.Chain chain, HttpServletRequest request) {
        return area(request).map(a -> a.chain() == chain).orElse(false);
    }

    public boolean owns(String areaName, HttpServletRequest request) {
        return area(request).map(a -> a.name().equals(areaName)).orElse(false);
    }

    public boolean permits(ApiRoute.Access access, HttpServletRequest request) {
        return route(request).map(r -> r.access() == access).orElse(false);
    }

    /** O caminho (com qualquer método) pertence a uma rota isenta da troca de senha pendente. */
    public boolean passwordChangeExempt(HttpServletRequest request) {
        var path = path(request);
        return routes.stream().anyMatch(c -> c.route().passwordChangeExempt() && c.pattern().matches(path));
    }

    public boolean jsonFailures(HttpServletRequest request) {
        return area(request).map(ApiArea::jsonFailures).orElse(false);
    }

    /** Padrões declarados por uma área, na ordem de declaração, para {@code securityMatcher}. */
    public String[] paths(String areaName) {
        return areas.stream().filter(a -> a.name().equals(areaName)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Área HTTP não declarada: " + areaName)).paths().toArray(String[]::new);
    }

    /** CORS de todas as áreas; origens explícitas, sem curinga. */
    public CorsConfigurationSource cors(List<String> origins) {
        if (origins.stream().anyMatch(value -> value.contains("*")))
            throw new IllegalArgumentException("CORS exige origens explícitas, sem wildcard");
        var source = new UrlBasedCorsConfigurationSource();
        for (var area : areas) {
            if (area.cors() == null) continue;
            var configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(origins);
            configuration.setAllowedMethods(area.cors().methods());
            configuration.setAllowCredentials(area.cors().credentials());
            configuration.setAllowedHeaders(area.cors().credentials()
                    ? List.of("Accept", "Content-Type", "X-CSRF-TOKEN") : List.of("Accept", "Content-Type"));
            for (var pattern : area.corsPaths()) source.registerCorsConfiguration(pattern, configuration);
        }
        return source;
    }

    private Optional<Compiled> compiled(HttpServletRequest request) {
        var path = path(request);
        return routes.stream().filter(c -> c.route().method().equals(request.getMethod()) && c.pattern().matches(path))
                .min((a, b) -> PathPattern.SPECIFICITY_COMPARATOR.compare(a.pattern(), b.pattern()));
    }

    /** Substitui cada variável ({@code {id}} ou {@code {id:regex}}, com chaves aninhadas) por {@code replacement}. */
    public static String normalized(String pattern, String replacement) {
        var result = new StringBuilder(); int depth = 0;
        for (var c : pattern.toCharArray()) {
            if (c == '{') { if (depth++ == 0) result.append(replacement); }
            else if (c == '}') depth--;
            else if (depth == 0) result.append(c);
        }
        return result.toString();
    }

    private static PathContainer path(HttpServletRequest request) { return PathContainer.parsePath(request.getServletPath()); }
}

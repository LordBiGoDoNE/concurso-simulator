package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.questionbank.application.port.AdministrativeImportPermission;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cadeia da curadoria administrativa (#177): consulta {@code /api/v1/admin/questions} e decisões
 * {@code /api/v1/admin/editorial-decisions}. Mesmo proprietário das decisões de acesso da importação:
 * sessão ADM, permissão/versão persistidas, troca inicial concluída, CSRF em POST e no-store.
 * Rotas e métodos fora da lista explícita são negados.
 */
@Configuration
public class AdministrativeCurationSecurityConfiguration {
    static final Pattern QUESTION = Pattern.compile("/api/v1/admin/questions(/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})?");
    static final Set<String> POSTS = Set.of("/api/v1/admin/questions/lookup", "/api/v1/admin/questions/export",
            "/api/v1/admin/editorial-decisions/preview", "/api/v1/admin/editorial-decisions/apply");

    @Bean @Order(0)
    SecurityFilterChain administrativeCurationSecurity(HttpSecurity http, AdministrativeImportPermission permission, ImportSettings settings,
            HttpSessionSecurityContextRepository repository, @Value("${app.cors-origins}") String origins) throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (cors.getAllowedOrigins().stream().anyMatch(o -> o.contains("*"))) throw new IllegalArgumentException("Origem explícita obrigatória");
        cors.setAllowCredentials(true); cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("Content-Type", "Accept", "X-CSRF-TOKEN"));
        return http.securityMatcher("/api/v1/admin/questions", "/api/v1/admin/questions/**", "/api/v1/admin/editorial-decisions", "/api/v1/admin/editorial-decisions/**")
                .cors(c -> c.configurationSource(r -> cors))
                .securityContext(c -> c.securityContextRepository(repository))
                .requestCache(c -> c.requestCache(new NullRequestCache()))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a -> a.requestMatchers(AdministrativeCurationSecurityConfiguration::allowedRoute)
                        .access((authentication, context) -> {
                            var current = authentication.get();
                            return new AuthorizationDecision(ImportSecurityConfiguration.valid(current)
                                    && current.getDetails() instanceof br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence evidence
                                    && permission.permits(((UserPrincipal) current.getPrincipal()).id(), evidence.version()));
                        }).anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((r, s, x) -> ImportSecurityConfiguration.reject(s, 401, "unauthenticated"))
                        .accessDeniedHandler((r, s, x) -> {
                            int status = x instanceof org.springframework.security.web.csrf.CsrfException ? 403
                                    : ImportSecurityConfiguration.valid(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()) ? 403 : 401;
                            ImportSecurityConfiguration.reject(s, status, status == 401 ? "unauthenticated" : "forbidden");
                        }))
                .addFilterBefore(new ImportSecurityConfiguration.NoStore(), org.springframework.web.filter.CorsFilter.class)
                .addFilterAfter(new BodyLimits(settings), AuthorizationFilter.class).build();
    }

    static boolean allowedRoute(HttpServletRequest request) {
        var path = request.getServletPath();
        return switch (request.getMethod()) {
            case "GET" -> QUESTION.matcher(path).matches();
            case "POST" -> POSTS.contains(path);
            default -> false;
        };
    }

    /** Depois da autorização: configuração das decisões em lote; só JSON UTF-8 sem compressão e corpo declarado até 2 MiB em POST. */
    static final class BodyLimits extends OncePerRequestFilter {
        private final ImportSettings settings;
        BodyLimits(ImportSettings settings) { this.settings = settings; }
        @Override protected boolean shouldNotFilter(HttpServletRequest r) { return !"POST".equals(r.getMethod()); }
        @Override protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain) throws ServletException, IOException {
            // Decisões em lote dependem do destino e da chave de confirmação; consultas não.
            if (r.getServletPath().startsWith("/api/v1/admin/editorial-decisions/") && !settings.configured()) {
                s.setStatus(503); s.setContentType("application/json"); s.setHeader("Cache-Control", "no-store");
                s.getWriter().write("{\"error\":\"DECISIONS_NOT_CONFIGURED\",\"correlationId\":\"" + UUID.randomUUID() + "\"}"); return;
            }
            try {
                var type = org.springframework.http.MediaType.parseMediaType(r.getContentType());
                if (!"application".equals(type.getType()) || !"json".equals(type.getSubtype())
                        || type.getCharset() != null && !java.nio.charset.StandardCharsets.UTF_8.equals(type.getCharset())) {
                    ImportSecurityConfiguration.reject(s, 415, "UNSUPPORTED_MEDIA_TYPE"); return;
                }
            } catch (IllegalArgumentException invalid) { ImportSecurityConfiguration.reject(s, 415, "UNSUPPORTED_MEDIA_TYPE"); return; }
            if (!"identity".equalsIgnoreCase(Optional.ofNullable(r.getHeader("Content-Encoding")).orElse("identity"))) {
                ImportSecurityConfiguration.reject(s, 415, "UNSUPPORTED_CONTENT_ENCODING"); return;
            }
            if (r.getContentLengthLong() > 2_097_152) { ImportSecurityConfiguration.reject(s, 413, "REQUEST_TOO_LARGE"); return; }
            chain.doFilter(r, s);
        }
    }
}

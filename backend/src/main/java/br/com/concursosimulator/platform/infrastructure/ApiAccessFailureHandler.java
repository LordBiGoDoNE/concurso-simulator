package br.com.concursosimulator.platform.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import br.com.concursosimulator.shared.infrastructure.http.JsonRejection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Rejeições HTTP da segurança, antes dos controllers; sem redirects ou dados pessoais.
 * 401 para rota declarada que exige identidade (fora de falha CSRF); 403 nos demais casos,
 * com corpo JSON somente nas áreas que o declaram.
 */
@Component
public final class ApiAccessFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ApiRoutes routes;

    public ApiAccessFailureHandler(ApiRoutes routes) {
        this.routes = routes;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        reject(request, response, false);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        reject(request, response, exception instanceof org.springframework.security.web.csrf.CsrfException);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, boolean csrf) throws IOException {
        if (!csrf && routes.route(request).map(ApiRoute::requiresIdentity).orElse(false)) {
            JsonRejection.write(response, 401, "UNAUTHENTICATED");
        } else {
            if (routes.jsonFailures(request)) JsonRejection.write(response, 403, "FORBIDDEN");
            else response.setStatus(403);
        }
    }
}

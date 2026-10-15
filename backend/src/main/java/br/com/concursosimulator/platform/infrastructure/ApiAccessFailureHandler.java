package br.com.concursosimulator.platform.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Rejeições HTTP da segurança, antes dos controllers; sem redirects ou dados pessoais. */
@Component
public final class ApiAccessFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
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
        boolean attempts=br.com.concursosimulator.mockexam.infrastructure.AttemptRoutes.owns(request);
        if (!csrf && ("GET".equals(request.getMethod()) && ("/api/v1/me".equals(request.getServletPath())
                || "/api/v1/me/attempts".equals(request.getServletPath()))
                || br.com.concursosimulator.mockexam.infrastructure.AttemptRoutes.permits(request))) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"error\":\"unauthenticated\"}");
        } else {
            response.setStatus(403);
            if (attempts) {
                response.setContentType("application/json"); response.setHeader("Cache-Control","no-store");
                response.getWriter().write("{\"error\":\"forbidden\"}");
            }
        }
    }
}

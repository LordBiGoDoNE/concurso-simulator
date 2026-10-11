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
        reject(request, response);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        reject(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("GET".equals(request.getMethod()) && "/api/v1/me".equals(request.getServletPath())) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"error\":\"unauthenticated\"}");
        } else {
            response.setStatus(403);
        }
    }
}

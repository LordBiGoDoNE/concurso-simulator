package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.infrastructure.QuestionBankRoutes;
import br.com.concursosimulator.shared.failure.TransientFailures;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import br.com.concursosimulator.shared.infrastructure.http.JsonRejection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/** A leitura/renovação da sessão ocorre fora do MVC e pode falhar antes da segurança. */
@Configuration
public class ImportAvailabilityConfiguration {
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> administrativeImportAvailability(
            @Value("${app.cors-origins}") String configuredOrigins, ApiRoutes routes) {
        var origins =
                Arrays.stream(configuredOrigins.split(",")).map(String::trim).toList();
        var filter = new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest r) {
                return !r.getServletPath().startsWith(ApiRoutes.ADMIN_NAMESPACE);
            }

            @Override
            protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
                    throws ServletException, IOException {
                s.setHeader("Cache-Control", "no-store");
                try {
                    chain.doFilter(r, s);
                } catch (RuntimeException | ServletException failure) {
                    boolean database = TransientFailures.transientCause(failure);
                    if (s.isCommitted() || !database) throw failure;
                    s.resetBuffer();
                    s.setHeader("Content-Length", null);
                    s.setStatus(503);
                    s.setContentType("application/json");
                    s.setHeader("Cache-Control", "no-store");
                    var origin = r.getHeader("Origin");
                    if (origin != null && origins.contains(origin)) {
                        s.setHeader("Access-Control-Allow-Origin", origin);
                        s.setHeader("Access-Control-Allow-Credentials", "true");
                        s.addHeader("Vary", "Origin");
                    }
                    JsonRejection.writeCorrelated(
                            s,
                            503,
                            routes.owns(QuestionBankRoutes.IMPORT_AREA, r)
                                    ? "IMPORT_UNAVAILABLE"
                                    : "ADMIN_UNAVAILABLE");
                }
            }
        };
        var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 9);
        return registration;
    }
}

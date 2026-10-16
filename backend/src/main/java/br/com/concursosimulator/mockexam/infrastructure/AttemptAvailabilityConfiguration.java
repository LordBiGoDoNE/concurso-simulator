package br.com.concursosimulator.mockexam.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Indisponibilidade de sessão ocorre antes do MVC; não deve virar 404, visitante ou sucesso. */
@Configuration
public class AttemptAvailabilityConfiguration {
    @Bean FilterRegistrationBean<OncePerRequestFilter> attemptAvailability() {
        var filter=new OncePerRequestFilter() {
            @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !AttemptRoutes.owns(request) && !br.com.concursosimulator.shared.infrastructure.SubjectReviewRoutes.owns(request); }
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                try { chain.doFilter(request,response); }
                catch (RuntimeException | ServletException failure) {
                    if (response.isCommitted() || !databaseFailure(failure)) throw failure;
                    response.resetBuffer();response.setHeader("Content-Length",null);response.setStatus(503);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
                    response.getWriter().write("{\"error\":\"unavailable\"}");
                }
            }
        };
        var registration=new FilterRegistrationBean<OncePerRequestFilter>(filter);registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER-10);return registration;
    }
    private static boolean databaseFailure(Throwable failure) {
        for (var cause=failure;cause!=null;cause=cause.getCause()) if (cause instanceof DataAccessException || cause instanceof TransactionException) return true;
        return false;
    }
}

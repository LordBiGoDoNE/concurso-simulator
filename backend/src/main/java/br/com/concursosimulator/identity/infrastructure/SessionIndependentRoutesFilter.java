package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

/** Bypasses both session lookup and commit, before Spring Session wraps the request. */
final class SessionIndependentRoutesFilter<S extends Session> extends SessionRepositoryFilter<S> {
    private final ApiRoutes routes;

    SessionIndependentRoutesFilter(SessionRepository<S> repository, ApiRoutes routes) {
        super(repository);
        this.routes = routes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (routes.owns(ApiArea.Chain.STATELESS, request) || OAuthRequestMethods.rejects(request)) {
            chain.doFilter(request, response);
        } else {
            super.doFilterInternal(request, response, chain);
        }
    }
}

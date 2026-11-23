package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** A substituted account must not continue studying under its previous UUID. */
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AdministrativeAccessUseCases access;
    private final ApiRoutes routes;
    public ActiveAccountFilter(AdministrativeAccessUseCases access,ApiRoutes routes){this.access=access;this.routes=routes;}
    @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws IOException,ServletException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof UserPrincipal p && !access.active(p.id())) {
            SecurityContextHolder.clearContext();var session=r.getSession(false);if(session!=null)session.invalidate();
            AdministrativeHttpSecurity.reject(s,401,"UNAUTHENTICATED");return;
        }
        if(auth!=null && auth.getPrincipal() instanceof UserPrincipal p && auth.getDetails() instanceof AdministrativeSecurity.Evidence epoch) {
            var current=access.googleAccess(p.id());
            if(current!=null && current.version()==epoch.version() && current.mustChange()
                    && !routes.passwordChangeExempt(r)) {
                AdministrativeHttpSecurity.reject(s,403,"PASSWORD_CHANGE_REQUIRED");return;
            }
        }
        chain.doFilter(r,s);
    }
}

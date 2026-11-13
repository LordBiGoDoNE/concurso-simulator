package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.web.UserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** A substituted account must not continue studying under its previous UUID. */
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AdministrativeAccessUseCases access;
    public ActiveAccountFilter(AdministrativeAccessUseCases access){this.access=access;}
    @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws IOException,ServletException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof UserPrincipal p && !access.active(p.id())) {
            SecurityContextHolder.clearContext();var session=r.getSession(false);if(session!=null)session.invalidate();
            AdministrativeHttpSecurity.reject(s,401,"UNAUTHENTICATED");return;
        }
        if(auth!=null && auth.getPrincipal() instanceof UserPrincipal p && auth.getDetails() instanceof AdministrativeSecurity.Evidence epoch) {
            var current=access.googleAccess(p.id());
            if(current!=null && current.version()==epoch.version() && current.mustChange()
                    && !java.util.Set.of("/api/v1/me","/api/v1/csrf","/api/v1/auth/config","/api/v1/auth/logout").contains(r.getServletPath())) {
                AdministrativeHttpSecurity.reject(s,403,"PASSWORD_CHANGE_REQUIRED");return;
            }
        }
        chain.doFilter(r,s);
    }
}

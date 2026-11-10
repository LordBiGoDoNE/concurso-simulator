package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases.Access;
import br.com.concursosimulator.identity.web.*;
import jakarta.servlet.http.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AdministrativeSecurity implements AdministrativeSession, AdministrativeGoogleCompletion {
    public static final String EPOCH="admin.epoch";
    public static final String LINK="admin.google-link";
    public record Evidence(long version) implements Serializable {}
    public record LinkIntent(UUID actor,long version,Instant expires,String state) implements Serializable {}
    private final AdministrativeAccessUseCases access;
    private final GoogleLoginProperties google;
    private final HttpSessionSecurityContextRepository repository;
    public AdministrativeSecurity(AdministrativeAccessUseCases access,GoogleLoginProperties google,HttpSessionSecurityContextRepository repository) {
        this.access=access;this.google=google;this.repository=repository;
    }
    public boolean permits(Authentication auth,boolean temporary) {
        if(!new org.springframework.security.authentication.AuthenticationTrustResolverImpl().isAuthenticated(auth) || !(auth.getPrincipal() instanceof UserPrincipal p) || !(auth.getDetails() instanceof Evidence e))return false;
        try { access.current(p.id(),e.version(),temporary); return true; }
        catch(AdministrativeAccessUseCases.Failure denied) { return false; }
    }
    public boolean authorize(Authentication auth,HttpServletRequest request) {
        if(!permits(auth,true))return false;
        request.setAttribute(EPOCH,((Evidence)auth.getDetails()).version());return true;
    }
    public void replace(HttpServletRequest request,HttpServletResponse response,Access a) {
        request.getSession();request.changeSessionId();
        request.getSession().removeAttribute(LINK);
        var minimal=UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(a.owner()),null,List.of(new SimpleGrantedAuthority("ROLE_USER")));
        minimal.setDetails(new Evidence(a.version()));
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(minimal);
        SecurityContextHolder.setContext(context);repository.saveContext(context,request,response);
        // Do not retain a CSRF token issued before the identity/session transition.
        new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository().saveToken(null,request,response);
    }
    public Authentication googleAuthentication(UUID user) {
        var auth=UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(user),null,List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var a=access.googleAccess(user);if(a!=null)auth.setDetails(new Evidence(a.version()));return auth;
    }

    @Override public boolean completeLink(HttpServletRequest request,HttpServletResponse response,br.com.concursosimulator.identity.domain.ExternalIdentity identity) throws java.io.IOException {
        var session=request.getSession(false);
        if(session==null || !(session.getAttribute(LINK) instanceof LinkIntent intent))return false;
        session.removeAttribute(LINK); // Consume before any mutation, including a conflict or failure.
        try {
            if(intent.state()==null || !intent.state().equals(request.getParameter("state")) || !Instant.now().isBefore(intent.expires()))
                throw new AdministrativeAccessUseCases.Failure("INVALID_LINK");
            var a=access.link(intent.actor(),intent.version(),identity);replace(request,response,a);
            response.sendRedirect(google.frontendUrl()+"?admin=linked");
        }catch(AdministrativeAccessUseCases.Failure failure) {
            // Only a still-valid source is restored; never restore stale authorization after a race.
            try{replace(request,response,access.current(intent.actor(),intent.version(),false));}
            catch(AdministrativeAccessUseCases.Failure invalid){org.springframework.security.core.context.SecurityContextHolder.clearContext();request.getSession().invalidate();}
            var code=Set.of("LOCAL_ACCOUNT_HAS_HISTORY","GOOGLE_ALREADY_LINKED").contains(failure.code())?failure.code():"LINK_FAILED";
            response.sendRedirect(google.frontendUrl()+"?admin="+code);
        }
        return true;
    }
    @Override public void replace(Access a) {
        var attributes=(ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
        replace(attributes.getRequest(),attributes.getResponse(),a);
    }
    @Override public String beginGoogleLink(Access a) {
        if(!google.enabled())throw new AdministrativeAccessUseCases.Failure("GOOGLE_DISABLED");
        var request=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        request.getSession().setAttribute(LINK,new LinkIntent(a.owner(),a.version(),Instant.now().plusSeconds(300),null));
        return "/oauth2/authorization/google";
    }
}

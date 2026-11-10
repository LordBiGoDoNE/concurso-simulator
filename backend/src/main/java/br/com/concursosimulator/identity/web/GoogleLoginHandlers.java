package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.ResolveExternalIdentityUseCase;
import br.com.concursosimulator.identity.application.port.LoginOptions;
import br.com.concursosimulator.identity.domain.ExternalIdentity;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final ResolveExternalIdentityUseCase identities;
    private final AdministrativeGoogleCompletion administrative;
    private final LoginOptions properties;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionOAuth2AuthorizedClientRepository clients;

    public GoogleLoginHandlers(ResolveExternalIdentityUseCase identities, LoginOptions properties,
                              HttpSessionSecurityContextRepository contexts, HttpSessionOAuth2AuthorizedClientRepository clients, AdministrativeGoogleCompletion administrative) {
        this.identities = identities;
        this.administrative = administrative;
        this.properties = properties;
        this.contexts = contexts;
        this.clients = clients;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken oauth)
                    || !oauth.getAuthorizedClientRegistrationId().equals("google")
                    || !(oauth.getPrincipal() instanceof OidcUser user)) {
                fail(request, response);
                return;
            }
            var external = new ExternalIdentity("google", "https://accounts.google.com", user.getSubject());
            if(administrative.completeLink(request,response,external)) {
                clients.removeAuthorizedClient("google",authentication,request,response);
                return;
            }
            var minimal = administrative.googleAuthentication(identities.execute(external));
            clients.removeAuthorizedClient("google", authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(minimal);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
        } catch (RuntimeException exception) {
            // Não registrar payload/exception OAuth: pode conter token, código ou subject.
            fail(request, response);
            return;
        }
        response.sendRedirect(properties.frontendUrl());
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException { fail(request, response); }

    private void fail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendRedirect(properties.frontendUrl() + "?auth=failed");
    }
}

package br.com.concursosimulator.identity;

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
    private final GoogleIdentityService identities;
    private final GoogleLoginProperties properties;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionOAuth2AuthorizedClientRepository clients;

    public GoogleLoginHandlers(GoogleIdentityService identities, GoogleLoginProperties properties,
                              HttpSessionSecurityContextRepository contexts, HttpSessionOAuth2AuthorizedClientRepository clients) {
        this.identities = identities;
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
            var principal = identities.resolveVerifiedSubject(user.getSubject());
            var minimal = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
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

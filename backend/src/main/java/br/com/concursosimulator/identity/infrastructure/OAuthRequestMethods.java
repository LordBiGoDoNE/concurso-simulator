package br.com.concursosimulator.identity.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

/** Technical route policy shared by the security chain and session boundary. */
public final class OAuthRequestMethods {
    private OAuthRequestMethods() { }

    public static boolean rejects(HttpServletRequest request) {
        return !"GET".equals(request.getMethod())
                && (IdentityRoutes.OAUTH_AUTHORIZATION.equals(request.getServletPath())
                    || IdentityRoutes.OAUTH_CALLBACK.equals(request.getServletPath()));
    }
}

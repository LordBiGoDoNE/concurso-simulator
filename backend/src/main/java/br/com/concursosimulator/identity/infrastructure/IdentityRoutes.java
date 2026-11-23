package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;

/** Rotas de sessão, OIDC e conta administrativa; único lugar que declara esses caminhos. */
@Configuration
public class IdentityRoutes {
    public static final String SESSION_AREA = "identity.session";
    public static final String ADMIN_AREA = "identity.admin-account";
    public static final String ME = "/api/v1/me";
    public static final String CSRF = "/api/v1/csrf";
    public static final String AUTH_CONFIG = "/api/v1/auth/config";
    public static final String LOGOUT = "/api/v1/auth/logout";
    /** Prefixo do resolvedor OAuth do Spring; o id do registro completa a rota. */
    public static final String OAUTH_AUTHORIZATION_BASE = "/oauth2/authorization";
    public static final String OAUTH_AUTHORIZATION = OAUTH_AUTHORIZATION_BASE + "/google";
    public static final String OAUTH_CALLBACK = "/login/oauth2/code/google";
    public static final String ADMIN_LOGIN = "/api/v1/admin/auth/login";
    public static final String ADMIN_ACCOUNT = "/api/v1/admin/account";
    public static final String ADMIN_PASSWORD = ADMIN_ACCOUNT + "/password";
    public static final String ADMIN_GOOGLE_LINK = ADMIN_ACCOUNT + "/google-link";

    @Bean
    ApiArea identitySessionArea() {
        return new ApiArea(SESSION_AREA, ApiArea.Chain.SESSION,
                List.of(ME, CSRF, AUTH_CONFIG, LOGOUT, OAUTH_AUTHORIZATION, OAUTH_CALLBACK),
                Cors.credentialed("GET", "POST"), List.of(ME, CSRF, LOGOUT, AUTH_CONFIG), false,
                List.of(ApiRoute.get(CSRF, PUBLIC).exemptFromPasswordChange(),
                        ApiRoute.get(AUTH_CONFIG, PUBLIC).exemptFromPasswordChange(),
                        // Atendida pelo LogoutFilter, depois do CSRF; a autorização nunca é consultada.
                        ApiRoute.post(LOGOUT, PUBLIC).exemptFromPasswordChange(),
                        ApiRoute.get(ME, ACCOUNT_PRINCIPAL).exemptFromPasswordChange(),
                        ApiRoute.get(OAUTH_AUTHORIZATION, PUBLIC),
                        ApiRoute.get(OAUTH_CALLBACK, PUBLIC)));
    }

    @Bean
    ApiArea identityAdministrativeArea() {
        var paths = List.of("/api/v1/admin/auth/**", ADMIN_ACCOUNT, ADMIN_ACCOUNT + "/**");
        return new ApiArea(ADMIN_AREA, ApiArea.Chain.OWN, paths, Cors.credentialed("GET", "POST"), paths, false,
                List.of(ApiRoute.post(ADMIN_LOGIN, PUBLIC),
                        ApiRoute.get(ADMIN_ACCOUNT, ADMINISTRATIVE),
                        ApiRoute.post(ADMIN_PASSWORD, ADMINISTRATIVE),
                        ApiRoute.post(ADMIN_GOOGLE_LINK, ADMINISTRATIVE)));
    }
}

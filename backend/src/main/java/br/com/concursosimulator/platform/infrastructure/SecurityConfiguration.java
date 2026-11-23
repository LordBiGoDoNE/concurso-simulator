package br.com.concursosimulator.platform.infrastructure;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.identity.infrastructure.ActiveAccountFilter;
import br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity;
import br.com.concursosimulator.identity.infrastructure.IdentityRoutes;
import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.mockexam.infrastructure.AttemptAccess;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import br.com.concursosimulator.identity.web.GoogleLoginHandlers;
import br.com.concursosimulator.identity.infrastructure.GoogleLoginProperties;
import br.com.concursosimulator.identity.infrastructure.OAuthRequestMethods;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfigurationSource;
import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;

/**
 * Compõe as cadeias de segurança a partir das áreas declaradas pelas funcionalidades ({@link ApiRoutes}).
 * Esta classe não declara caminhos de rota.
 */
@Configuration
public class SecurityConfiguration {
    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    // Local ADM uses its dedicated authentication chain; suppress Boot's unrelated default user.
    @Bean
    UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Login não configurado"); };
    }

    @Bean
    @Order(1)
    SecurityFilterChain statelessSecurityFilterChain(HttpSecurity http, ApiAccessFailureHandler failures, ApiRoutes routes) throws Exception {
        // Áreas sem sessão (prontidão, questões públicas): só leem ou negam; logout/OIDC mantêm proteção própria.
        return http.securityMatcher(r -> routes.owns(ApiArea.Chain.STATELESS, r))
                .cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers(r -> routes.permits(PUBLIC, r)).permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain rejectedOAuthMethodsSecurityFilterChain(HttpSecurity http, ApiAccessFailureHandler failures)
            throws Exception {
        // This chain cannot execute an operation: reject before OAuth or session/CSRF materialization.
        return http.securityMatcher(OAuthRequestMethods::rejects)
                .cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain securityFilterChain(HttpSecurity http, HttpSessionSecurityContextRepository repository,
            GoogleLoginProperties google, ObjectProvider<ClientRegistrationRepository> registrations,
            GoogleLoginHandlers handlers, HttpSessionOAuth2AuthorizedClientRepository clients,
            ApiAccessFailureHandler failures, AttemptAccess attempts, AdministrativeAccessUseCases adminAccess,
            ApiRoutes routes) throws Exception {
        if (google.enabled()) {
            var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations.getObject(), IdentityRoutes.OAUTH_AUTHORIZATION_BASE);
            resolver.setAuthorizationRequestCustomizer(builder -> {
                OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
                var attrs=(org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                var session=attrs.getRequest().getSession(false);
                if(session!=null && session.getAttribute(AdministrativeSecurity.LINK) instanceof AdministrativeSecurity.LinkIntent intent) {
                    if(java.time.Instant.now().isAfter(intent.expires()) || intent.state()!=null)
                        throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("Invalid link intent");
                    builder.additionalParameters(p->p.put("prompt","select_account"));
                    session.setAttribute(AdministrativeSecurity.LINK,
                        new AdministrativeSecurity.LinkIntent(intent.actor(),intent.version(),intent.expires(),builder.build().getState()));
                }
            });
            http.oauth2Login(login -> login.authorizedClientRepository(clients)
                    .loginProcessingUrl(IdentityRoutes.OAUTH_CALLBACK)
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                    .successHandler(handlers).failureHandler(handlers));
        }
        var trust = new AuthenticationTrustResolverImpl();
        return http.addFilterAfter(new ActiveAccountFilter(adminAccess, routes), org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(repository))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(r -> routes.permits(PUBLIC, r)).permitAll()
                        .requestMatchers(r -> routes.permits(ACCOUNT, r)).access((a,r) -> attempts.authorize(a.get(),r.getRequest(),true))
                        .requestMatchers(r -> routes.permits(VISITOR_OR_ACCOUNT, r)).access((a,r) -> attempts.authorize(a.get(),r.getRequest(),false))
                        .requestMatchers(r -> routes.permits(ACCOUNT_PRINCIPAL, r)).access((authentication, request) -> {
                            var current = authentication.get();
                            return new AuthorizationDecision(trust.isAuthenticated(current)
                                    && current.getPrincipal() instanceof UserPrincipal);
                        })
                        .anyRequest().denyAll())
                .logout(logout -> logout.logoutUrl(IdentityRoutes.LOGOUT)
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(failures).accessDeniedHandler(failures))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins, ApiRoutes routes) {
        return routes.cors(Arrays.stream(origins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList());
    }
}

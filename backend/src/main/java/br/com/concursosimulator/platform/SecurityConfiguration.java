package br.com.concursosimulator.platform;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import br.com.concursosimulator.identity.GoogleLoginHandlers;
import br.com.concursosimulator.identity.GoogleLoginProperties;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    // Nenhum usuário ou login neste incremento; impede a conta padrão gerada pelo Boot.
    @Bean
    UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Login não configurado"); };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, HttpSessionSecurityContextRepository repository,
            GoogleLoginProperties google, ObjectProvider<ClientRegistrationRepository> registrations,
            GoogleLoginHandlers handlers, HttpSessionOAuth2AuthorizedClientRepository clients) throws Exception {
        if (google.enabled()) {
            var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations.getObject(), "/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
            http.oauth2Login(login -> login.authorizedClientRepository(clients)
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                    .successHandler(handlers).failureHandler(handlers));
        }
        return http.cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(repository))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/status", "/api/v1/csrf", "/api/v1/auth/config",
                                "/oauth2/authorization/google", "/login/oauth2/code/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").authenticated()
                        .anyRequest().denyAll())
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getServletPath().equals("/api/v1/me")) {
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.setHeader("Cache-Control", "no-store");
                                response.getWriter().write("{\"error\":\"unauthenticated\"}");
                            } else response.setStatus(403);
                        }))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
        var configuration = new CorsConfiguration();
        var allowed = Arrays.stream(origins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (allowed.stream().anyMatch(value -> value.contains("*"))) {
            throw new IllegalArgumentException("CORS exige origens explícitas, sem wildcard");
        }
        configuration.setAllowedOrigins(allowed);
        configuration.setAllowedMethods(List.of("GET"));
        configuration.setAllowedHeaders(List.of("Accept", "Content-Type"));
        configuration.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/status", configuration);
        var sessions = new CorsConfiguration(configuration);
        sessions.setAllowCredentials(true);
        sessions.setAllowedMethods(List.of("GET", "POST"));
        sessions.setAllowedHeaders(List.of("Accept", "Content-Type", "X-CSRF-TOKEN"));
        source.registerCorsConfiguration("/api/v1/me", sessions);
        source.registerCorsConfiguration("/api/v1/csrf", sessions);
        source.registerCorsConfiguration("/api/v1/auth/logout", sessions);
        source.registerCorsConfiguration("/api/v1/auth/config", sessions);
        return source;
    }
}

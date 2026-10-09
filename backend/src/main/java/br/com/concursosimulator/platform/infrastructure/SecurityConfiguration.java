package br.com.concursosimulator.platform.infrastructure;

import br.com.concursosimulator.identity.web.UserPrincipal;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
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
            ApiAccessFailureHandler failures) throws Exception {
        var trust = new AuthenticationTrustResolverImpl();
        return http.cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(repository))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/status", "/api/v1/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/me").access((authentication, request) -> {
                            var current = authentication.get();
                            return new AuthorizationDecision(trust.isAuthenticated(current)
                                    && current.getPrincipal() instanceof UserPrincipal);
                        })
                        .anyRequest().denyAll())
                .logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(failures).accessDeniedHandler(failures))
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
        return source;
    }
}

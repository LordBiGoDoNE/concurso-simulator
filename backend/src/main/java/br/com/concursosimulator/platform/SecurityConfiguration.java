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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    // Nenhum usuário ou login neste incremento; impede a conta padrão gerada pelo Boot.
    @Bean
    UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Login não configurado"); };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/status").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(403)))
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
        return source;
    }
}

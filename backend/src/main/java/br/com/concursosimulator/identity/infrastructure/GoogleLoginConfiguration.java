package br.com.concursosimulator.identity.infrastructure;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;

@Configuration
@EnableConfigurationProperties(GoogleLoginProperties.class)
public class GoogleLoginConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.google.enabled", havingValue = "true")
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository googleRegistration(GoogleLoginProperties properties, Environment environment) {
        validate(properties, environment);
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(properties.clientId()).clientSecret(properties.clientSecret())
                .scope("openid", "profile").redirectUri(properties.callbackUrl())
                .issuerUri("https://accounts.google.com").build());
    }

    static void validate(GoogleLoginProperties properties, Environment environment) {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalArgumentException("Login Google habilitado exige client ID e secret externos");
        }
        URI callback = browserUri(properties.callbackUrl(), environment);
        browserUri(properties.frontendUrl(), environment);
        if (!"/login/oauth2/code/google".equals(callback.getPath())) {
            throw new IllegalArgumentException("Callback Google deve usar /login/oauth2/code/google");
        }
    }

    private static URI browserUri(String value, Environment environment) {
        URI uri;
        try { uri = URI.create(value == null ? "" : value); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("URL de login inválida"); }
        boolean local = environment.acceptsProfiles(Profiles.of("local"))
                && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || local && "http".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("URLs de login exigem HTTPS; HTTP somente em localhost no perfil local");
        }
        return uri;
    }

    @Bean
    HttpSessionOAuth2AuthorizedClientRepository authorizedClients() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }
}

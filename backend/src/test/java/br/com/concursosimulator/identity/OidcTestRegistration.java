package br.com.concursosimulator.identity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@TestConfiguration(proxyBeanMethods = false)
public class OidcTestRegistration {
    @Bean
    ClientRegistrationRepository fixtureRegistration(@Value("${fixture.issuer}") String issuer,
                                                     @Value("${app.google.callback-url}") String callback) {
        return new InMemoryClientRegistrationRepository(ClientRegistrations.fromIssuerLocation(issuer)
                .registrationId("google").clientId("test-client").clientSecret("fixture-only-secret")
                .scope("openid", "profile").redirectUri(callback).build());
    }
}

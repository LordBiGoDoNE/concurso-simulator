package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.infrastructure.GoogleLoginConfiguration;
import br.com.concursosimulator.identity.infrastructure.GoogleLoginProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import static org.assertj.core.api.Assertions.assertThat;

class GoogleConfigurationTests {
    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GoogleLoginConfiguration.class);

    @Test
    void disabledNeedsNoCredentialsOrNetwork() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(ClientRegistrationRepository.class);
            assertThat(context.getBean(GoogleLoginProperties.class).enabled()).isFalse();
        });
    }

    @Test
    void enabledRejectsMissingCredentialsAndUnsafeUrls() {
        runner.withPropertyValues("app.google.enabled=true").run(context -> assertThat(context).hasFailed());
        for (String url : new String[]{"", "http://example.com", "https://user:pass@example.com",
                "https://example.com?returnTo=evil", "https://example.com#fragment"}) {
            configured().withPropertyValues("app.google.frontend-url=" + url)
                    .run(context -> assertThat(context).hasFailed());
        }
        configured().withPropertyValues("app.google.callback-url=https://example.com/wrong")
                .run(context -> assertThat(context).hasFailed());
        configured().withPropertyValues("app.google.callback-url=http://127.0.0.1:8080/login/oauth2/code/google")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validGoogleRegistrationIsFixedAndSecretIsNotPrinted() {
        configured().run(context -> {
            assertThat(context).hasNotFailed();
            var registration = context.getBean(ClientRegistrationRepository.class).findByRegistrationId("google");
            assertThat(registration.getProviderDetails().getIssuerUri()).isEqualTo("https://accounts.google.com");
            assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "profile");
            assertThat(context.getBean(GoogleLoginProperties.class).toString()).doesNotContain("fixture-secret");
        });
        configured().withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .withPropertyValues("app.google.callback-url=http://127.0.0.1:8080/login/oauth2/code/google",
                        "app.google.frontend-url=http://127.0.0.1:5173")
                .run(context -> assertThat(context).hasNotFailed());
    }

    ApplicationContextRunner configured() {
        return runner.withPropertyValues("app.google.enabled=true", "app.google.client-id=fixture-id",
                "app.google.client-secret=fixture-secret", "app.google.callback-url=https://api.example.com/login/oauth2/code/google",
                "app.google.frontend-url=https://app.example.com");
    }
}

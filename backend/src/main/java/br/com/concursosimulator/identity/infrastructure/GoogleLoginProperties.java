package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.port.LoginOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.google")
public record GoogleLoginProperties(boolean enabled, String clientId, String clientSecret,
                                    String callbackUrl, String frontendUrl) implements LoginOptions {
    @Override
    public String toString() { return "GoogleLoginProperties[enabled=" + enabled + "]"; }
}

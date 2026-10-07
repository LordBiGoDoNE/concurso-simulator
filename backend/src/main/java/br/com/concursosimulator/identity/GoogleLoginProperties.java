package br.com.concursosimulator.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.google")
public record GoogleLoginProperties(boolean enabled, String clientId, String clientSecret,
                                    String callbackUrl, String frontendUrl) {
    @Override
    public String toString() { return "GoogleLoginProperties[enabled=" + enabled + "]"; }
}

package br.com.concursosimulator.identity.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfiguration {
    @Bean
    DefaultCookieSerializer cookieSerializer(Environment environment) {
        var cookie = new DefaultCookieSerializer();
        cookie.setCookieName("SESSION");
        cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true);
        cookie.setUseSecureCookie(!environment.acceptsProfiles(Profiles.of("local")));
        cookie.setSameSite("Lax");
        cookie.setUseBase64Encoding(false);
        return cookie;
    }
}

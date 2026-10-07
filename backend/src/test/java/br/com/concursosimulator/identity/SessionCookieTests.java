package br.com.concursosimulator.identity;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;
import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieTests {
    @Test
    void cookiesAreSecureUnlessLocalProfileIsExplicit() {
        for (String profile : new String[]{"production", "default", "local"}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            var response = new MockHttpServletResponse();
            new SessionConfiguration().cookieSerializer(environment).writeCookieValue(
                    new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, "session-id"));
            String cookie = response.getHeader("Set-Cookie");
            assertThat(cookie).contains("HttpOnly", "SameSite=Lax").doesNotContain("Domain=");
            if (profile.equals("local")) assertThat(cookie).doesNotContain("Secure");
            else assertThat(cookie).contains("Secure");
        }
    }
}

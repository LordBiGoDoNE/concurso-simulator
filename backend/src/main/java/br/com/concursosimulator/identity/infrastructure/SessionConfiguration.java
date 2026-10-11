package br.com.concursosimulator.identity.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;

@Configuration
public class SessionConfiguration {
    @Bean
    CookieHttpSessionIdResolver sessionIdResolver(DefaultCookieSerializer cookie) {
        var resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(cookie);
        return resolver;
    }

    @Bean
    static BeanPostProcessor sessionRouteBoundary(ConfigurableListableBeanFactory beans) {
        return new BeanPostProcessor() {
            @Override
            @SuppressWarnings("unchecked")
            public Object postProcessAfterInitialization(Object bean, String name) {
                if ("springSessionRepositoryFilter".equals(name) && bean instanceof SessionRepositoryFilter<?>) {
                    // Spring Session 4.1.1 exposes only the repository and ID resolver as filter configuration.
                    // Keep Boot's single proxy registration and order; replace its target, not the registration.
                    return sessionFilter(beans.getBean(SessionRepository.class), beans.getBean(HttpSessionIdResolver.class));
                }
                return bean;
            }
        };
    }

    private static <S extends Session> SessionRepositoryFilter<S> sessionFilter(
            SessionRepository<S> repository, HttpSessionIdResolver sessionIds) {
        var filter = new SessionIndependentRoutesFilter<>(repository);
        filter.setHttpSessionIdResolver(sessionIds);
        return filter;
    }

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

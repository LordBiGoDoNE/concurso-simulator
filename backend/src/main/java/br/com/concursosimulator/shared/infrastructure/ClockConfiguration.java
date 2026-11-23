package br.com.concursosimulator.shared.infrastructure;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Único relógio da aplicação; casos de uso recebem {@link Clock} e testes podem substituí-lo. */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

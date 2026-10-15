package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.application.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

@Configuration @EnableScheduling
public class AttemptConfiguration {
    @Bean AttemptUseCases attemptUseCases(AttemptStore store) {
        return new AttemptUseCases(store,Clock.systemUTC(),UUID::randomUUID,SecureRandom::new);
    }
    @Bean ExpiredAttemptCleanup expiredAttemptCleanup(AttemptStore store) {
        return new ExpiredAttemptCleanup(store,Clock.systemUTC());
    }
}

package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.application.*;
import org.springframework.context.annotation.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class AttemptConfiguration {
    @Bean AttemptUseCases attemptUseCases(AttemptStore store) {
        return new AttemptUseCases(store,Clock.systemUTC(),UUID::randomUUID,SecureRandom::new);
    }
}

package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.application.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AttemptConfiguration {
    @Bean
    @Primary
    AttemptUseCases attemptUseCases(
            AttemptStore store,
            @org.springframework.beans.factory.annotation.Value("${parameterization.generation-enabled:true}")
                    boolean generationEnabled,
            Clock clock) {
        return new AttemptUseCases(store, clock, UUID::randomUUID, SecureRandom::new, generationEnabled);
    }

    @Bean("lessonPracticeUseCases")
    AttemptUseCases lessonPracticeUseCases(AttemptStore store, Clock clock) {
        return new AttemptUseCases(
                store,
                clock,
                UUID::randomUUID,
                SecureRandom::new,
                false,
                br.com.concursosimulator.questionbank.domain.QuestionPurpose.PRATICA_AULA);
    }

    @Bean
    ExpiredAttemptCleanup expiredAttemptCleanup(AttemptStore store, Clock clock) {
        return new ExpiredAttemptCleanup(store, clock);
    }
}

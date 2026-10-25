package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.application.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

@Configuration @EnableScheduling
public class AttemptConfiguration {
    @Bean @Primary AttemptUseCases attemptUseCases(AttemptStore store,
            @org.springframework.beans.factory.annotation.Value("${parameterization.generation-enabled:true}") boolean generationEnabled) {
        return new AttemptUseCases(store,Clock.systemUTC(),UUID::randomUUID,SecureRandom::new,generationEnabled);
    }
    @Bean("lessonPracticeUseCases") AttemptUseCases lessonPracticeUseCases(AttemptStore store) {
        return new AttemptUseCases(store,Clock.systemUTC(),UUID::randomUUID,SecureRandom::new,false,
                br.com.concursosimulator.questionbank.domain.QuestionPurpose.PRATICA_AULA);
    }
    @Bean ExpiredAttemptCleanup expiredAttemptCleanup(AttemptStore store) {
        return new ExpiredAttemptCleanup(store,Clock.systemUTC());
    }
}

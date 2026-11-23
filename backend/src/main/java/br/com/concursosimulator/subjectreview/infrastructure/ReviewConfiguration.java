package br.com.concursosimulator.subjectreview.infrastructure;

import br.com.concursosimulator.mockexam.application.CompletedAttemptReader;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class ReviewConfiguration {
    @Bean
    ReviewUseCases reviewUseCases(CompletedAttemptReader reader, Clock clock) {
        return new ReviewUseCases(reader, clock);
    }
}

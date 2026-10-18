package br.com.concursosimulator.subjectreview.infrastructure;

import br.com.concursosimulator.mockexam.application.CompletedAttemptReader;
import br.com.concursosimulator.subjectreview.application.ReviewUseCases;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class ReviewConfiguration {
    @Bean ReviewUseCases reviewUseCases(CompletedAttemptReader reader) {
        return new ReviewUseCases(reader,Clock.systemUTC());
    }
}

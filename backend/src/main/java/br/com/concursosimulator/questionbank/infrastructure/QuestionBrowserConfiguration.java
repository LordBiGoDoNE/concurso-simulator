package br.com.concursosimulator.questionbank.infrastructure;

import br.com.concursosimulator.questionbank.application.BrowseQuestionsUseCase;
import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import org.springframework.context.annotation.*;

@Configuration
public class QuestionBrowserConfiguration {
    @Bean BrowseQuestionsUseCase browseQuestions(EligibleQuestions questions) { return new BrowseQuestionsUseCase(questions); }
}

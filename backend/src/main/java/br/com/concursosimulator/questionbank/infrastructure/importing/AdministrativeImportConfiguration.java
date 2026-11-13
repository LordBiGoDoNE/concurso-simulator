package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.*;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AdministrativeImportConfiguration {
    @Bean AdministrativePackageReader administrativePackageReader(){return new AdministrativePackageReader();}
    @Bean QuestionImportTarget questionImportTarget(ImportSettings settings){return new QuestionImportTarget(settings.destination());}
    @Bean PreviewQuestionCandidatesUseCase previewAdministrativeQuestions(EntityManager em,PlatformTransactionManager manager){
        return new PreviewQuestionCandidatesUseCase(new JpaQuestionInventory(em,manager));
    }
    @Bean ApplyAdministrativeQuestionsUseCase applyAdministrativeQuestions(QuestionImportStore store){return new ApplyAdministrativeQuestionsUseCase(store,Clock.systemUTC());}
    @Bean ImportConfirmationTokens importConfirmationTokens(ImportSettings settings){return new ImportConfirmationTokens(settings.secret(),Clock.systemUTC());}
}

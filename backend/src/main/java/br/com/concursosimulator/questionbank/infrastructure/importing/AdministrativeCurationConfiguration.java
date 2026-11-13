package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.application.AdministrativeQuestionQueries;
import br.com.concursosimulator.questionbank.infrastructure.persistence.*;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

/** Wiring da curadoria administrativa (#177); núcleo permanece Java puro. */
@Configuration
public class AdministrativeCurationConfiguration {
    @Bean JpaAdministrativeQuestionCatalog administrativeQuestionCatalog(EntityManager em, PlatformTransactionManager manager, JpaQuestionStore store) {
        return new JpaAdministrativeQuestionCatalog(em, manager, store);
    }
    @Bean AdministrativeQuestionQueries administrativeQuestionQueries(JpaAdministrativeQuestionCatalog catalog, EntityManager em,
            PlatformTransactionManager manager, AdministrativePackageReader reader) {
        return new AdministrativeQuestionQueries(catalog, new JpaQuestionInventory(em, manager), reader);
    }
}

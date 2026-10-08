package br.com.concursosimulator.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTests {
    private static final JavaClasses CODE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("br.com.concursosimulator");

    @Test
    void coreIsFrameworkIndependentAndDoesNotDependOnAdapters() {
        classes().that().resideInAnyPackage("..domain..", "..application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..", "..application..")
                .check(CODE);
        noClasses().that().resideInAnyPackage("..domain..", "..application..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..",
                        "org.hibernate..", "jakarta.servlet..", "java.sql..", "javax.sql..", "..infrastructure..", "..web..")
                .check(CODE);
    }

    @Test
    void domainDoesNotDependOnApplication() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..").check(CODE);
    }

    @Test
    void webDoesNotAccessPersistenceImplementations() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "jakarta.persistence..",
                        "org.hibernate..", "org.springframework.jdbc..", "org.springframework.data..", "java.sql..", "javax.sql..")
                .check(CODE);
    }

    @Test
    void jpaModelsAreInfrastructureOnly() {
        classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .or().areAnnotatedWith(jakarta.persistence.Embeddable.class)
                .should().resideInAPackage("..infrastructure.persistence..").check(CODE);
    }

    @Test
    void functionalModulesHaveNoCycles() {
        slices().matching("br.com.concursosimulator.(*)..").should().beFreeOfCycles().check(CODE);
    }
}

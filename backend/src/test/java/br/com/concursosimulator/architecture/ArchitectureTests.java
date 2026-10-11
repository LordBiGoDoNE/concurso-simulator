package br.com.concursosimulator.architecture;

import br.com.concursosimulator.identity.web.SessionController;
import br.com.concursosimulator.identity.web.GoogleLoginController;
import br.com.concursosimulator.identity.web.AuthConfigResponse;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;
import java.lang.reflect.ParameterizedType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void sessionControllerDoesNotOwnAuthenticationOrCsrfMechanisms() {
        // Escopo intencional: handlers/callbacks OIDC podem consumir Authentication.
        noClasses().that().haveFullyQualifiedName(SessionController.class.getName())
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.security.core", "org.springframework.security.core.context..",
                        "org.springframework.security.authentication..", "jakarta.servlet.http..")
                .check(CODE);
        noClasses().that().haveFullyQualifiedName(SessionController.class.getName())
                .should().dependOnClassesThat().haveFullyQualifiedName(
                        "org.springframework.security.web.csrf.CsrfTokenRepository")
                .orShould().dependOnClassesThat().haveFullyQualifiedName(
                        "org.springframework.security.web.csrf.CsrfFilter")
                .check(CODE);
    }

    @Test
    void authConfigurationHasAConcreteWebContract() throws Exception {
        var method = GoogleLoginController.class.getDeclaredMethod("config");
        assertThat(method.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(method.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        var response = (ParameterizedType) method.getGenericReturnType();
        assertThat(response.getRawType()).isEqualTo(ResponseEntity.class);
        assertThat(response.getActualTypeArguments()).containsExactly(AuthConfigResponse.class);
        assertThat(AuthConfigResponse.class.isRecord()).isTrue();
    }

    @Test
    void sessionHttpResponsesUseConcreteWebRecords() {
        // Protege este contrato estável, sem impor record a payloads dinâmicos ou outros adaptadores.
        var endpoints = java.util.Arrays.stream(SessionController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class)).toList();
        assertThat(endpoints).hasSize(2);
        for (var endpoint : endpoints) {
            assertThat(endpoint.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
            var response = (ParameterizedType) endpoint.getGenericReturnType();
            assertThat(response.getRawType()).isEqualTo(ResponseEntity.class);
            assertThat(response.getActualTypeArguments()).hasSize(1);
            assertThat(response.getActualTypeArguments()[0]).isInstanceOf(Class.class);
            var payload = (Class<?>) response.getActualTypeArguments()[0];
            assertThat(payload.isRecord()).isTrue();
            assertThat(payload.getPackageName()).isEqualTo(SessionController.class.getPackageName());
        }
    }
}

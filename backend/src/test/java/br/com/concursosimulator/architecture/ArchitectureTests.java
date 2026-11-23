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
    void questionCoreOwnsItsRulesWithoutDependingOnIdentityOrAdapters() {
        classes().that().resideInAnyPackage("..questionbank.domain..", "..questionbank.application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..", "..questionbank.domain..", "..questionbank.application..").check(CODE);
    }

    @Test void mockExamCoreConsumesQuestionAndParameterizationDomainWithoutAdaptersOrReverseDependency() {
        classes().that().resideInAnyPackage("..mockexam.domain..", "..mockexam.application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..", "..mockexam.domain..", "..mockexam.application..", "..questionbank.domain..",
                        "..parameterization.domain..").check(CODE);
    }
    @Test void parameterizationCoreOnlyDependsOnItsOwnCoreAndQuestionValueObjects() {
        classes().that().resideInAnyPackage("..parameterization.domain..", "..parameterization.application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..", "..parameterization.domain..", "..parameterization.application..", "..questionbank.domain..").check(CODE);
    }
    @Test void subjectReviewCoreConsumesOnlyCompletedEvidenceAndQuestionValueObjects() {
        classes().that().resideInAnyPackage("..subjectreview.domain..", "..subjectreview.application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..", "..subjectreview.domain..", "..subjectreview.application..", "..questionbank.domain..",
                        "..mockexam.domain..", "..mockexam.application..").check(CODE);
    }
    private static final String AUTHENTICATION_PRINCIPAL = "org.springframework.security.core.annotation.AuthenticationPrincipal";

    /** Exceção nominal à regra de controllers: motivo e únicos tipos extras de segurança permitidos. */
    private record ControllerSecurityException(String reason, java.util.Set<String> extraSecurityTypes) {}

    private static final java.util.Map<String, ControllerSecurityException> CONTROLLER_SECURITY_EXCEPTIONS = java.util.Map.of(
            "br.com.concursosimulator.identity.web.SessionController", new ControllerSecurityException(
                    "expõe o CsrfToken gerado pelo framework",
                    java.util.Set.of("org.springframework.security.web.csrf.CsrfToken")));

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> outsideSessionServletAndSecurityExcept(
            java.util.Set<String> allowedSecurityTypes) {
        return com.tngtech.archunit.base.DescribedPredicate.describe(
                "fora de mecanismos de sessão/servlet/segurança, exceto " + new java.util.TreeSet<>(allowedSecurityTypes),
                (com.tngtech.archunit.core.domain.JavaClass target) -> {
                    var name = target.getName();
                    if (name.startsWith("org.springframework.session.") || name.startsWith("jakarta.servlet.")) return false;
                    if (!name.startsWith("org.springframework.security.")) return true;
                    return allowedSecurityTypes.contains(name);
                });
    }

    @Test void everyControllerReceivesVerifiedIdentityWithoutOwningSessionOrServletMechanics() {
        classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().onlyDependOnClassesThat(outsideSessionServletAndSecurityExcept(java.util.Set.of(AUTHENTICATION_PRINCIPAL)))
                .check(CODE.that(com.tngtech.archunit.base.DescribedPredicate.describe("sem exceção nominal",
                        (com.tngtech.archunit.core.domain.JavaClass c) -> !CONTROLLER_SECURITY_EXCEPTIONS.containsKey(c.getName()))));
        // Cada exceção só ganha os tipos nomeados no mapa, nada mais de segurança/sessão/servlet.
        CONTROLLER_SECURITY_EXCEPTIONS.forEach((controller, exception) -> {
            assertThat(CODE.contain(controller)).as("exceção nominal sem classe correspondente: %s", controller).isTrue();
            var allowed = new java.util.HashSet<>(exception.extraSecurityTypes());
            allowed.add(AUTHENTICATION_PRINCIPAL);
            classes().that().haveFullyQualifiedName(controller)
                    .should().onlyDependOnClassesThat(outsideSessionServletAndSecurityExcept(allowed))
                    .because(exception.reason())
                    .check(CODE);
        });
    }

    @Test void everyMappedMethodReturnsAConcreteRecordFromItsOwnWebPackageOrNoBody() {
        var controllers = CODE.stream().filter(c -> c.isAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)).toList();
        assertThat(controllers).hasSizeGreaterThanOrEqualTo(11);
        var violations = new java.util.ArrayList<String>();
        for (var controller : controllers) {
            var type = controller.reflect();
            for (var method : type.getDeclaredMethods()) {
                if (java.util.Arrays.stream(method.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(
                        org.springframework.web.bind.annotation.RequestMapping.class)
                        || a.annotationType().equals(org.springframework.web.bind.annotation.RequestMapping.class))) continue;
                if (!(method.getGenericReturnType() instanceof ParameterizedType response) || response.getRawType() != ResponseEntity.class
                        || !(response.getActualTypeArguments()[0] instanceof Class<?> body)) {
                    violations.add(type.getSimpleName() + "." + method.getName() + ": retorno não é ResponseEntity<Classe>");
                    continue;
                }
                if (body == Void.class) continue; // sem corpo (204/404)
                if (!body.isRecord() || !(body.getPackageName().equals(type.getPackageName())
                        || body.getPackageName().equals("br.com.concursosimulator.shared.web")))
                    violations.add(type.getSimpleName() + "." + method.getName() + ": " + body.getName());
            }
        }
        assertThat(violations).as("métodos HTTP sem DTO record do próprio pacote web").isEmpty();
    }

    @Test void controllersAndAdvicesResideInWebAdapters() {
        classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .or().areAnnotatedWith(org.springframework.stereotype.Controller.class)
                .or().areAnnotatedWith(org.springframework.web.bind.annotation.RestControllerAdvice.class)
                .or().areAnnotatedWith(org.springframework.web.bind.annotation.ControllerAdvice.class)
                .should().resideInAPackage("..web..").check(CODE);
    }

    @Test void infrastructureDoesNotDependOnWebAdapters() {
        noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("br.com.concursosimulator..web..").check(CODE);
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

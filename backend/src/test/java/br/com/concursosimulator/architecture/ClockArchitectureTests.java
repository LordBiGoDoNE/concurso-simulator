package br.com.concursosimulator.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.concursosimulator.shared.infrastructure.ClockConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Um único relógio: só {@link ClockConfiguration} cria o {@link Clock} do sistema; o resto recebe o bean.
 * Limite: não cobre {@code Instant.now()} sem relógio (expiração de intenções OAuth na segurança, #266).
 */
class ClockArchitectureTests {
    private static final JavaClasses CODE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.concursosimulator");

    static final ArchRule SYSTEM_CLOCK_ONLY_IN_CONFIGURATION = noClasses()
            .that()
            .doNotHaveFullyQualifiedName(ClockConfiguration.class.getName())
            .should()
            .callMethod(Clock.class, "systemUTC")
            .orShould()
            .callMethod(Clock.class, "systemDefaultZone")
            .orShould()
            .callMethod(Clock.class, "system", ZoneId.class)
            .because("o relógio da aplicação é o bean Clock de ClockConfiguration");

    @Test
    void onlyClockConfigurationCreatesTheSystemClock() {
        SYSTEM_CLOCK_ONLY_IN_CONFIGURATION.check(CODE);
    }

    @Test
    void ruleRejectsAnotherSystemClock() {
        var violation = new ClassFileImporter().importClasses(OwnClock.class);
        assertThatThrownBy(() -> SYSTEM_CLOCK_ONLY_IN_CONFIGURATION.check(violation))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(OwnClock.class.getName());
    }

    static class OwnClock {
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}

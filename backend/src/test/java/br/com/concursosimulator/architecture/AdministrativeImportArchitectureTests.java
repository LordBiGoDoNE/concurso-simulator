package br.com.concursosimulator.architecture;

import br.com.concursosimulator.questionbank.web.AdministrativeImportController;
import org.junit.jupiter.api.Test;
import java.lang.reflect.ParameterizedType;
import com.tngtech.archunit.core.importer.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.*;

class AdministrativeImportArchitectureTests {
    @Test void controllerDoesNotPersistOrOwnSessionAndUsesConcreteWebRecords(){
        var code=new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("br.com.concursosimulator");
        noClasses().that().haveFullyQualifiedName(AdministrativeImportController.class.getName()).should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..","..infrastructure.persistence..","org.springframework.session..","jakarta.servlet..","org.springframework.security.web.csrf..").check(code);
        for(var method:AdministrativeImportController.class.getDeclaredMethods())if(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)){
            var response=(ParameterizedType)method.getGenericReturnType();assertThat(response.getRawType()).isEqualTo(org.springframework.http.ResponseEntity.class);
            assertThat(response.getActualTypeArguments()[0]).isInstanceOf(Class.class);assertThat(((Class<?>)response.getActualTypeArguments()[0]).isRecord()).isTrue();
        }
    }
}

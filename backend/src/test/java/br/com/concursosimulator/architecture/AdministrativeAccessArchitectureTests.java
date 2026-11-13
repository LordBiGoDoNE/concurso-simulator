package br.com.concursosimulator.architecture;

import br.com.concursosimulator.identity.web.AdministrativeAccountController;
import com.tngtech.archunit.core.importer.*;
import java.lang.reflect.ParameterizedType;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class AdministrativeAccessArchitectureTests {
    @Test void accountControllerReceivesVerifiedIdentityAndUsesConcreteDtosWithoutOwningSessionMechanics(){
        var code=new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("br.com.concursosimulator");
        noClasses().that().haveFullyQualifiedName(AdministrativeAccountController.class.getName()).should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..","jakarta.servlet..","org.springframework.security.core.context..","org.springframework.security.authentication..").check(code);
        for(var method:AdministrativeAccountController.class.getDeclaredMethods()) {
            if(java.util.Arrays.stream(method.getAnnotations()).noneMatch(a->a.annotationType().isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class)))continue;
            var response=(ParameterizedType)method.getGenericReturnType();assertThat(response.getRawType()).isEqualTo(ResponseEntity.class);
            assertThat(response.getActualTypeArguments()[0]).isInstanceOf(Class.class);
            var body=(Class<?>)response.getActualTypeArguments()[0];assertThat(body==Void.class || body.isRecord()).isTrue();
        }
    }
}

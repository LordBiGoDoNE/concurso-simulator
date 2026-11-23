package br.com.concursosimulator.architecture;

import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import br.com.concursosimulator.questionbank.web.AdministrativeImportController;
import br.com.concursosimulator.questionbank.web.ImportRequestBodyAdvice;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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

    /**
     * O 422 do pacote é decidido pelo advice antes do controller. Se ele validasse contra tipos do núcleo
     * (QuestionImportReader.Document, objetos de valor do domínio), renomear o núcleo mudaria o contrato de
     * entrada sem tocar o mapeador web. Só a exceção InvalidPackage do núcleo é permitida.
     */
    @Test void importBodyAdviceValidatesAgainstWebContractNotCoreTypes(){
        var code=new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("br.com.concursosimulator");
        var reader=QuestionImportReader.class.getName();
        var coreContract=DescribedPredicate.describe("tipos do contrato do núcleo",(JavaClass c)->
                c.getPackageName().startsWith("br.com.concursosimulator.questionbank.domain")
                        || c.getName().startsWith(reader+"$") && !c.isEquivalentTo(QuestionImportReader.InvalidPackage.class));
        noClasses().that().haveFullyQualifiedName(ImportRequestBodyAdvice.class.getName()).should().dependOnClassesThat(coreContract).check(code);
    }
}

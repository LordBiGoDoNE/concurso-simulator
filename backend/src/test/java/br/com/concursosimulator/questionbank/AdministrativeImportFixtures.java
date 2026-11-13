package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Dados sintéticos exclusivos; aprovação destes dados nunca constitui curadoria real. */
public final class AdministrativeImportFixtures {
    private AdministrativeImportFixtures(){}
    public static AdministrativePackageReader.Document document(QuestionPurpose purpose,String...keys){
        var bytes="Fonte sintética privada PRIVATE_SENTINEL".getBytes(StandardCharsets.UTF_8);
        var hash=AdministrativePackageReader.sha(bytes);var file=new AdministrativePackageReader.File("sources/fixture.txt","text/plain",hash,Base64.getEncoder().encodeToString(bytes));
        var items=new ArrayList<AdministrativePackageReader.Item>();
        for(var key:keys){
            var old=QuestionFixtures.content("Fixture "+key+" a < b > c");
            var provenance=new Provenance("Teste sintético","sources/fixture.txt","sources/fixture.txt",hash,hash);
            var c=new QuestionContent(old.statement(),old.alternatives(),old.correctAlternativeId(),old.resolution(),old.classification(),old.references(),provenance);
            var content=new QuestionImportReader.Content(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                    new QuestionImportReader.Classification("portugues","P01",List.of("assistente-administrativo","secretario-de-escola")),c.references(),provenance);
            items.add(new AdministrativePackageReader.Item(new QuestionOrigin(purpose==QuestionPurpose.SIMULADO?"admin-question-v1":"lesson-practice-v1",key),content,c.fingerprint(),null));
        }
        return new AdministrativePackageReader.Document(AdministrativePackageReader.FORMAT,purpose,items,List.of(file));
    }
}

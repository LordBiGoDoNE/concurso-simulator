package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Dados sintéticos exclusivos; aprovação destes dados nunca constitui curadoria real. */
public final class AdministrativeImportFixtures {
    private AdministrativeImportFixtures(){}
    private static final tools.jackson.databind.json.JsonMapper JSON=tools.jackson.databind.json.JsonMapper.builder().build();
    /** Pacote no contrato web (DTO), pelo mesmo JSON que o cliente envia. */
    public static br.com.concursosimulator.questionbank.web.AdministrativeContracts.PackageDocument web(Object document){
        return JSON.convertValue(document,br.com.concursosimulator.questionbank.web.AdministrativeContracts.PackageDocument.class);
    }
    /** Pacote no contrato da aplicação, pelo mesmo JSON. */
    public static QuestionImportReader.Document application(Object document){
        return JSON.convertValue(document,QuestionImportReader.Document.class);
    }
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
    /** Como {@link #document}, com recurso (PNG declarado em files) no enunciado e referência com base legal. */
    public static AdministrativePackageReader.Document rich(QuestionPurpose purpose,String...keys){return rich(purpose,"",keys);}
    /** Variante com sufixo no enunciado: mesmo pacote de arquivos, conteúdo (e fingerprint) diferente. */
    public static AdministrativePackageReader.Document rich(QuestionPurpose purpose,String statementSuffix,String...keys){
        var base=document(purpose,keys);
        var png=new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82};
        var pngHash=AdministrativePackageReader.sha(png);
        var asset=new AdministrativePackageReader.File("assets/figura-sintetica.png","image/png",pngHash,Base64.getEncoder().encodeToString(png));
        var resource=new TextContent.Resource("assets/figura-sintetica.png","image/png",pngHash,"Figura sintética: dois quadrados lado a lado");
        var reference=new StudyReference("Referência legal sintética",java.net.URI.create("https://example.test/lei-sintetica"),
                new StudyReference.LegalBasis("Art. 1º da lei sintética",java.time.LocalDate.of(2026,1,15)));
        var items=new ArrayList<AdministrativePackageReader.Item>();
        for(var item:base.items()){
            var c=item.content();
            var statement=new TextContent(c.statement().text()+statementSuffix,List.of(resource));
            var references=new ArrayList<>(c.references());references.add(reference);
            var k=c.classification();
            var content=new QuestionContent(statement,c.alternatives(),c.correctAlternativeId(),c.resolution(),
                    QuestionClassification.fromIds(k.disciplineId(),k.topicId(),k.roleIds()),references,c.provenance());
            items.add(new AdministrativePackageReader.Item(item.origin(),new QuestionImportReader.Content(statement,c.alternatives(),c.correctAlternativeId(),
                    c.resolution(),k,references,c.provenance()),content.fingerprint(),null));
        }
        var files=new ArrayList<>(base.files());files.add(asset);
        return new AdministrativePackageReader.Document(base.format(),purpose,items,files);
    }
}

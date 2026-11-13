package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.*;
import static org.assertj.core.api.Assertions.*;

/** Synthetic vectors: all accepted semantic fields, including unselected evidence. */
class AdministrativeDigestTests {
    final JsonMapper json=JsonMapper.builder().build();
    final AdministrativePackageReader reader=new AdministrativePackageReader();
    record Mutation(String name,Consumer<ObjectNode> apply) {}

    @Test void everyContentFieldAndEditorialListChangesTheFullPackageDigest(){
        for(var purpose:QuestionPurpose.values())for(int index=0;index<2;index++) {
            var original=fixture(purpose);var baseline=digest(original);String p="/items/"+index+"/content";
            var mutations=new ArrayList<Mutation>();
            for(String field:List.of("statement/text","resolution/text","alternatives/0/content/text"))
                mutations.add(new Mutation(field,r->put(r,p+"/"+field,"Texto sintético diferente")));
            mutations.add(new Mutation("statement.whitespace",r->put(r,p+"/statement/text",original.at(p+"/statement/text").asString()+" ")));
            mutations.add(new Mutation("statement.unicode",r->put(r,p+"/statement/text",java.text.Normalizer.normalize(original.at(p+"/statement/text").asString(),java.text.Normalizer.Form.NFD))));
            mutations.add(new Mutation("alternative.id",r->{
                String id="20000000-0000-0000-0000-000000000001";
                put(r,p+"/alternatives/0/id",id);put(r,p+"/correctAlternativeId",id);
            }));
            mutations.add(new Mutation("correctAlternativeId",r->put(r,p+"/correctAlternativeId",QuestionFixtures.SECOND.toString())));
            mutations.add(new Mutation("alternative.order",r->reverse(r,p+"/alternatives")));
            mutations.add(new Mutation("topic",r->put(r,p+"/classification/topicId","P02")));
            mutations.add(new Mutation("discipline/topic",r->{put(r,p+"/classification/disciplineId","matematica-raciocinio");put(r,p+"/classification/topicId","M01");}));
            mutations.add(new Mutation("roles",r->((ArrayNode)r.at(p+"/classification/roleIds")).remove(1)));
            for(String field:List.of("name","url","legalBasis/provision","legalBasis/checkedOn")) {
                String value=switch(field){case "url"->"https://example.test/other";case "legalBasis/checkedOn"->"2026-11-10";default->"Outra referência sintética";};
                mutations.add(new Mutation("reference."+field,r->put(r,p+"/references/0/"+field,value)));
            }
            mutations.add(new Mutation("legalBasis.presence",r->((ObjectNode)r.at(p+"/references/0")).putNull("legalBasis")));
            mutations.add(new Mutation("reference.order",r->reverse(r,p+"/references")));
            mutations.add(new Mutation("provenance.author",r->put(r,p+"/provenance/author","Outra autoria sintética")));
            for(String block:List.of("statement","resolution","alternatives/0/content")) {
                mutations.add(new Mutation(block+".resource.description",r->put(r,p+"/"+block+"/resources/0/description","Outro diagrama")));
                mutations.add(new Mutation(block+".resource.order",r->reverse(r,p+"/"+block+"/resources")));
                mutations.add(new Mutation(block+".resource.count",r->((ArrayNode)r.at(p+"/"+block+"/resources")).remove(1)));
            }
            for(var mutation:mutations){var changed=original.deepCopy();mutation.apply().accept(changed);
                assertThat(digest(changed)).as("%s item=%s %s",purpose,index,mutation.name()).isNotEqualTo(baseline);
            }
        }
    }

    @Test void originBasePurposeAndEveryEvidenceFieldAreBoundEvenWhenNotSelected(){
        for(var purpose:QuestionPurpose.values()) {
            var original=fixture(purpose);String baseline=digest(original);
            var mutations=new ArrayList<Mutation>();
            mutations.add(new Mutation("unselected.origin.key",r->put(r,"/items/1/origin/key","admin.other")));
            mutations.add(new Mutation("unselected.expectedLatestRevision",r->put(r,"/items/1/expectedLatestRevision","30000000-0000-0000-0000-000000000001")));
            mutations.add(new Mutation("purpose/namespace",r->{
                boolean practice=purpose==QuestionPurpose.SIMULADO;put(r,"/purpose",practice?"PRATICA_AULA":"SIMULADO");
                for(int i=0;i<2;i++)put(r,"/items/"+i+"/origin/namespace",practice?"lesson-practice-v1":"admin-question-v1");
            }));
            // Files 2/3 belong exclusively to the unselected item; resources are shared.
            for(int file=0;file<6;file++) {
                int f=file;
                mutations.add(new Mutation("file."+f+".path/provenance/resource",r->renameFile(r,f)));
                mutations.add(new Mutation("file."+f+".bytes/hash/length",r->changeFile(r,f,false)));
                if(f<4)mutations.add(new Mutation("file."+f+".mediaType",r->put(r,"/files/"+f+"/mediaType","application/json")));
                else mutations.add(new Mutation("file."+f+".mediaType/resource",r->changeFile(r,f,true)));
            }
            for(var mutation:mutations){var changed=original.deepCopy();mutation.apply().accept(changed);
                assertThat(digest(changed)).as("%s %s",purpose,mutation.name()).isNotEqualTo(baseline);
            }
            var pack=reader.read(signed(original),List.of("admin.a"));
            assertThat(pack.evidence()).extracting(e->e.path()).doesNotContain("sources/1-content.txt","sources/1-origin.txt");
        }
    }

    @Test void canonicalVectorsAndDeclaredSetEquivalencesRemainStable(){
        for(var purpose:QuestionPurpose.values()) {
            var root=fixture(purpose);var expected=digest(root);
            assertThat(expected).isEqualTo(purpose==QuestionPurpose.SIMULADO
                    ?"58c9032a28da0138fb6c11680f8fee1ae517de67e7a5b2f79cb06ae6bf583b26"
                    :"b167e64968b33b21e51bca3f196d6e5739f897500f1b56b562f07ba8740f2bf8");
            var changed=root.deepCopy();reverse(changed,"/items");reverse(changed,"/files");
            for(int i=0;i<2;i++)reverse(changed,"/items/"+i+"/content/classification/roleIds");
            assertThat(digest(changed)).isEqualTo(expected);
            var doc=signed(root);var all=reader.read(doc,List.of("admin.a","admin.b"));
            assertThat(reader.read(doc,List.of("admin.b","admin.a")).selectionDigest()).isEqualTo(all.selectionDigest());
            assertThat(all.digest()).isEqualTo(expected);
            assertThat(all.selectionDigest()).isEqualTo("1b175376c5ca0f860444f614a22d4ff03bc70a3fef3c5215296af48ea7781c16");
            assertThat(reader.read(doc,List.of("admin.a")).selectionDigest()).isNotEqualTo(all.selectionDigest());
            var reordered=json.readTree(" \n"+json.writeValueAsString(changed)+"\n ");
            assertThat(digest((ObjectNode)reordered)).isEqualTo(expected);
        }
    }

    ObjectNode fixture(QuestionPurpose purpose){
        var files=new ArrayList<AdministrativePackageReader.File>();
        var resources=new ArrayList<TextContent.Resource>();
        for(int i=0;i<2;i++) {
            byte[] bytes=("<svg xmlns=\"http://www.w3.org/2000/svg\"><title>Diagrama "+i+"</title></svg>").getBytes(StandardCharsets.UTF_8);
            var file=file("assets/diagram"+i+".svg","image/svg+xml",bytes);
            resources.add(new TextContent.Resource(file.path(),file.mediaType(),file.sha256(),"Diagrama "+i));
        }
        var items=new ArrayList<AdministrativePackageReader.Item>();
        for(int i=0;i<2;i++) {
            var source=file("sources/"+i+"-content.txt","text/plain","{}".getBytes(StandardCharsets.UTF_8));
            var answer=file("sources/"+i+"-origin.txt","text/plain","{}".getBytes(StandardCharsets.UTF_8));
            files.add(source);files.add(answer);var old=QuestionFixtures.content("Questão "+i);
            var content=new QuestionImportReader.Content(new TextContent(old.statement().text(),resources),
                    old.alternatives().stream().map(a->new QuestionContent.Alternative(a.id(),new TextContent(a.content().text(),resources))).toList(),old.correctAlternativeId(),
                    new TextContent(old.resolution().text(),resources),new QuestionImportReader.Classification("portugues","P01",List.of("assistente-administrativo","secretario-de-escola")),
                    List.of(new StudyReference("Estudo",URI.create("https://example.test/study"),new StudyReference.LegalBasis("art. 1",LocalDate.of(2026,11,9))),new StudyReference("Outro estudo",URI.create("https://example.test/extra"),null)),
                    new Provenance("Autoria sintética",source.path(),answer.path(),source.sha256(),answer.sha256()));
            items.add(new AdministrativePackageReader.Item(new QuestionOrigin(purpose==QuestionPurpose.SIMULADO?"admin-question-v1":"lesson-practice-v1","admin."+(i==0?"a":"b")),content,"0".repeat(64),null));
        }
        for(int i=0;i<2;i++)files.add(file(resources.get(i).path(),"image/svg+xml",("<svg xmlns=\"http://www.w3.org/2000/svg\"><title>Diagrama "+i+"</title></svg>").getBytes(StandardCharsets.UTF_8)));
        return (ObjectNode)json.valueToTree(new AdministrativePackageReader.Document(AdministrativePackageReader.FORMAT,purpose,items,files));
    }
    AdministrativePackageReader.File file(String path,String type,byte[] bytes){return new AdministrativePackageReader.File(path,type,AdministrativePackageReader.sha(bytes),Base64.getEncoder().encodeToString(bytes));}
    AdministrativePackageReader.Document signed(ObjectNode root){
        var d=json.treeToValue(root,AdministrativePackageReader.Document.class);
        return new AdministrativePackageReader.Document(d.format(),d.purpose(),d.items().stream().map(i->{var c=i.content();
            var domain=new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),QuestionClassification.fromIds(c.classification().disciplineId(),c.classification().topicId(),c.classification().roleIds()),c.references(),c.provenance());
            return new AdministrativePackageReader.Item(i.origin(),c,domain.fingerprint(),i.expectedLatestRevision());}).toList(),d.files());
    }
    String digest(ObjectNode root){return reader.read(signed(root),List.of("admin.a")).digest();}
    void put(ObjectNode root,String path,String value){int slash=path.lastIndexOf('/');((ObjectNode)root.at(path.substring(0,slash))).put(path.substring(slash+1),value);}
    void reverse(ObjectNode root,String path){var array=(ArrayNode)root.at(path);var copy=new ArrayList<JsonNode>();array.forEach(copy::add);array.removeAll();copy.reversed().forEach(array::add);}
    void renameFile(ObjectNode root,int index){
        var f=(ObjectNode)root.at("/files/"+index);String old=f.get("path").asString(),value=old+".new";f.put("path",value);
        for(var item:root.get("items")){
            var p=(ObjectNode)item.at("/content/provenance");for(var name:List.of("contentPath","answerPath"))if(p.get(name).asString().equals(old))p.put(name,value);
            resources(item,r->{if(r.get("path").asString().equals(old))r.put("path",value);});
        }
    }
    void changeFile(ObjectNode root,int index,boolean png){
        var f=(ObjectNode)root.at("/files/"+index);String path=f.get("path").asString();
        byte[] bytes=png?new byte[]{(byte)137,80,78,71,13,10,26,10}:(index<4?"{ }":"<svg xmlns=\"http://www.w3.org/2000/svg\"><title>Outro diagrama</title></svg>").getBytes(StandardCharsets.UTF_8);
        String hash=AdministrativePackageReader.sha(bytes);f.put("sha256",hash);f.put("base64",Base64.getEncoder().encodeToString(bytes));if(png)f.put("mediaType","image/png");
        for(var item:root.get("items")) {
            var p=(ObjectNode)item.at("/content/provenance");if(p.get("contentPath").asString().equals(path))p.put("contentSha256",hash);if(p.get("answerPath").asString().equals(path))p.put("answerSha256",hash);
            resources(item,r->{if(r.get("path").asString().equals(path)){r.put("sha256",hash);if(png)r.put("mediaType","image/png");}});
        }
    }
    void resources(JsonNode item,Consumer<ObjectNode> action){
        for(var block:List.of(item.at("/content/statement"),item.at("/content/resolution"),item.at("/content/alternatives/0/content"),item.at("/content/alternatives/1/content")))
            block.get("resources").forEach(r->action.accept((ObjectNode)r));
    }
}

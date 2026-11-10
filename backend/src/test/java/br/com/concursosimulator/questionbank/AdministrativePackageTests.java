package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static br.com.concursosimulator.questionbank.AdministrativeImportFixtures.document;

class AdministrativePackageTests {
    final AdministrativePackageReader reader=new AdministrativePackageReader();
    @Test void maximumDecodedEvidenceIsAcceptedAndExcessRejected(){
        var keys=java.util.stream.IntStream.range(0,100).mapToObj(n->"admin.max"+n).toArray(String[]::new);
        var d=document(QuestionPurpose.PRATICA_AULA,keys);byte[] bytes=new byte[1_048_576];Arrays.fill(bytes,(byte)'a');
        var hash=AdministrativePackageReader.sha(bytes);var encoded=Base64.getEncoder().encodeToString(bytes);
        var files=new ArrayList<AdministrativePackageReader.File>();
        for(int i=0;i<8;i++)files.add(new AdministrativePackageReader.File("sources/max"+i,"text/plain",hash,encoded));
        var items=new ArrayList<AdministrativePackageReader.Item>();
        for(int i=0;i<100;i++){
            var item=d.items().get(i);var c=item.content();var path="sources/max"+(i%8);var p=new Provenance("Fixture",path,path,hash,hash);
            var content=new br.com.concursosimulator.questionbank.application.port.QuestionImportReader.Content(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),c.classification(),c.references(),p);
            var domain=new QuestionContent(content.statement(),content.alternatives(),content.correctAlternativeId(),content.resolution(),QuestionClassification.fromIds("portugues","P01",content.classification().roleIds()),content.references(),p);
            items.add(new AdministrativePackageReader.Item(item.origin(),content,domain.fingerprint(),null));
        }
        var max=new AdministrativePackageReader.Document(d.format(),d.purpose(),items,files);
        long heapBefore=java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long begin=System.nanoTime();var pack=reader.read(max,List.of(keys));
        assertThat(pack.candidates()).hasSize(100);assertThat(pack.evidence().stream().mapToInt(f->f.bytes().length).sum()).isEqualTo(8_388_608);
        System.out.println("Synthetic max-package check items=100 decodedBytes=8388608 elapsedMs="+(System.nanoTime()-begin)/1_000_000);
        System.out.println("Synthetic max-package heapBefore="+heapBefore+" heapAfter="+java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        files.add(new AdministrativePackageReader.File("sources/excess","text/plain",hash,encoded));
        assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),d.purpose(),items,files),List.of(keys))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
    }
    @Test void bothCollectionsHaveStableDigestsWithoutAlteringEditorialText(){
        for(var purpose:QuestionPurpose.values()){
            var doc=document(purpose,"admin.a","admin.b");var pack=reader.read(doc,List.of("admin.b","admin.a"));
            assertThat(pack.candidates()).hasSize(2).allSatisfy(c->{assertThat(c.origin().purpose()).isEqualTo(purpose);assertThat(c.content().statement().text()).contains("a < b > c");});
            var reversed=new AdministrativePackageReader.Document(doc.format(),purpose,doc.items().reversed(),doc.files());
            assertThat(reader.read(reversed,List.of("admin.a","admin.b")).digest()).isEqualTo(pack.digest());
            assertThat(reader.read(doc,List.of("admin.a")).selectionDigest()).isNotEqualTo(pack.selectionDigest());
            assertThat(reader.read(doc,List.of("admin.a")).digest()).isEqualTo(pack.digest());
            assertThat(pack.evidence()).hasSize(1);var bytes=pack.evidence().getFirst().bytes();bytes[0]=0;
            assertThat(pack.evidence().getFirst().bytes()[0]).isNotZero();
        }
    }
    @Test void rejectsPurposeMismatchHistoricalPracticeKeysUnknownKeysAndFingerprint(){
        var d=document(QuestionPurpose.SIMULADO,"admin.a");
        assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),QuestionPurpose.PRATICA_AULA,d.items(),d.files()),List.of("admin.a"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        assertThatThrownBy(()->reader.read(document(QuestionPurpose.PRATICA_AULA,"P01-P01"),List.of("P01-P01"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        for(var keys:List.of(List.of("missing"),List.of("admin.a","admin.a"),List.<String>of()))assertThatThrownBy(()->reader.read(d,keys)).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        var i=d.items().getFirst();var bad=new AdministrativePackageReader.Item(i.origin(),i.content(),"f".repeat(64),null);
        assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),d.purpose(),List.of(bad),d.files()),List.of("admin.a"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
    }
    @Test void validatesUnselectedContentAndBaseParticipatesInDigest(){
        var d=document(QuestionPurpose.SIMULADO,"admin.a","admin.b");var b=d.items().getLast();
        var based=new AdministrativePackageReader.Item(b.origin(),b.content(),b.fingerprint(),UUID.randomUUID());
        var changed=new AdministrativePackageReader.Document(d.format(),d.purpose(),List.of(d.items().getFirst(),based),d.files());
        assertThat(reader.read(changed,List.of("admin.a")).digest()).isNotEqualTo(reader.read(d,List.of("admin.a")).digest());
        var invalid=new AdministrativePackageReader.Item(b.origin(),b.content(),"0".repeat(64),null);
        assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),d.purpose(),List.of(d.items().getFirst(),invalid),d.files()),List.of("admin.a"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
    }
    @Test void refusesExtraFilesTraversalCorruptionAndTooManyItems(){
        var d=document(QuestionPurpose.SIMULADO,"admin.a");var file=d.files().getFirst();
        for(var bad:List.of(new AdministrativePackageReader.File("sources/../secret","text/plain",file.sha256(),file.base64()),
                new AdministrativePackageReader.File(file.path(),file.mediaType(),"0".repeat(64),file.base64()),
                new AdministrativePackageReader.File(file.path(),file.mediaType(),file.sha256(),file.base64()+"\n")))
            assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),d.purpose(),d.items(),List.of(bad)),List.of("admin.a"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        var extra=new AdministrativePackageReader.File("sources/unused",file.mediaType(),file.sha256(),file.base64());
        assertThatThrownBy(()->reader.read(new AdministrativePackageReader.Document(d.format(),d.purpose(),d.items(),List.of(file,extra)),List.of("admin.a"))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        var keys=java.util.stream.IntStream.range(0,101).mapToObj(n->"admin."+n).toArray(String[]::new);
        assertThatThrownBy(()->reader.read(document(QuestionPurpose.PRATICA_AULA,keys),List.of(keys[0]))).isInstanceOf(AdministrativePackageReader.InvalidPackage.class);
        var hundred=document(QuestionPurpose.PRATICA_AULA,Arrays.copyOf(keys,100));
        assertThat(reader.read(hundred,hundred.items().stream().map(i->i.origin().key()).toList()).candidates()).hasSize(100);
    }

}

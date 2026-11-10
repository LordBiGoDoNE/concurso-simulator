package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.local.PackageContentSafety;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Entrada autossuficiente: caminhos são chaves, nunca caminhos do host. */
public final class AdministrativePackageReader implements QuestionImportReader {
    public static final String FORMAT="administrative-question-package/v1";
    public QuestionImportPackage read(Document document,List<String> keys) {
        try{return validated(document,keys);}
        catch(InvalidPackage error){throw error;}
        catch(IllegalArgumentException | NullPointerException | IOException error){throw new InvalidPackage();}
    }
    private QuestionImportPackage validated(Document d,List<String> keys)throws IOException {
        require(FORMAT.equals(d.format()) && d.purpose()!=null && d.items()!=null && d.items().size()>=1 && d.items().size()<=100);
        require(keys!=null && !keys.isEmpty() && keys.size()<=100 && new HashSet<>(keys).size()==keys.size());
        require(d.files()!=null && d.files().size()<=100);
        var files=new TreeMap<String,QuestionImportPackage.Evidence>();int total=0;
        for(var f:d.files()) {
            require(f.path()!=null && f.path().length()<=1000 && (f.path().startsWith("sources/") || f.path().startsWith("assets/")));
            require(!f.path().contains("\\") && !f.path().contains(":") && !f.path().contains("%"));
            for(var part:f.path().split("/",-1))require(!part.isBlank() && !Set.of(".","..").contains(part));
            require(f.base64()!=null && f.base64().length()<=1_398_104);
            byte[] bytes=Base64.getDecoder().decode(f.base64());
            require(bytes.length<=1_048_576 && Base64.getEncoder().encodeToString(bytes).equals(f.base64()));
            total=Math.addExact(total,bytes.length);require(total<=8_388_608 && sha(bytes).equals(f.sha256()));
            if(f.path().startsWith("sources/")) {
                require(Set.of("text/plain","application/json").contains(f.mediaType()));
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes));
            } else require(Set.of("image/png","image/jpeg","image/svg+xml").contains(f.mediaType()));
            require(files.put(f.path(),new QuestionImportPackage.Evidence(f.path(),f.mediaType(),f.sha256(),bytes))==null);
        }
        var candidates=new TreeMap<String,ApplyQuestionCandidatesUseCase.Candidate>();
        var uses=new HashMap<String,Set<String>>();var allUsed=new HashSet<String>();
        for(var item:d.items()) {
            var origin=Objects.requireNonNull(item.origin());var key=origin.key();
            require(key.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}"));
            require(d.purpose()==QuestionPurpose.SIMULADO?origin.namespace().equals("admin-question-v1"):
                    origin.namespace().equals(QuestionPurpose.PRACTICE_NAMESPACE) && key.startsWith("admin.") && key.length()>6);
            var c=item.content();
            require(c.alternatives().size()>=2 && c.alternatives().size()<=10 && c.references().size()>=1 && c.references().size()<=20);
            var content=new QuestionContent(c.statement(),c.alternatives(),c.correctAlternativeId(),c.resolution(),
                    QuestionClassification.fromIds(c.classification().disciplineId(),c.classification().topicId(),c.classification().roleIds()),c.references(),c.provenance());
            require(content.fingerprint().equals(item.fingerprint()));
            var used=new HashSet<String>();var provenance=content.provenance();
            source(files,used,provenance.contentPath(),provenance.contentSha256());
            source(files,used,provenance.answerPath(),provenance.answerSha256());
            var blocks=new ArrayList<TextContent>();blocks.add(content.statement());blocks.add(content.resolution());
            content.alternatives().forEach(a->blocks.add(a.content()));
            for(var block:blocks) {
                require(block.resources().size()<=8);
                for(var resource:block.resources()) {
                    var file=files.get(resource.path());
                    require(file!=null && file.sha256().equals(resource.sha256()) && file.mediaType().equals(resource.mediaType()));
                    used.add(resource.path());
                }
            }
            PackageContentSafety.check(path->{var file=files.get(path);require(file!=null);return file.bytes();},content);
            require(candidates.put(key,new ApplyQuestionCandidatesUseCase.Candidate(origin,content,item.expectedLatestRevision()))==null);
            uses.put(key,used);allUsed.addAll(used);
        }
        require(files.keySet().equals(allUsed) && candidates.keySet().containsAll(keys));
        var selected=keys.stream().sorted().map(candidates::get).toList();
        var selectedFiles=new TreeSet<String>();keys.forEach(k->selectedFiles.addAll(uses.get(k)));
        var duplicates=new ArrayList<List<String>>();var byText=new HashMap<String,List<String>>();
        candidates.forEach((k,c)->byText.computeIfAbsent(c.content().statement().text(),ignore->new ArrayList<>()).add(k));
        byText.values().stream().filter(group->group.size()>1).sorted(Comparator.comparing(List::getFirst)).forEach(duplicates::add);
        String digest=digest(out->{
            text(out,FORMAT);text(out,d.purpose().name());out.writeInt(candidates.size());
            for(var candidate:candidates.values()) {
                text(out,candidate.origin().namespace());text(out,candidate.origin().key());text(out,candidate.content().fingerprint());
                out.writeBoolean(candidate.expectedLatestRevision()!=null);
                if(candidate.expectedLatestRevision()!=null)text(out,candidate.expectedLatestRevision().toString());
            }
            out.writeInt(files.size());
            for(var file:files.values()){text(out,file.path());text(out,file.mediaType());text(out,file.sha256());out.writeInt(file.bytes().length);}
        });
        String selection=digest(out->{text(out,"question-import-selection/v1");out.writeInt(keys.size());for(var key:keys.stream().sorted().toList())text(out,key);});
        return new QuestionImportPackage(d.purpose(),digest,selection,selected,selectedFiles.stream().map(files::get).toList(),duplicates);
    }
    private static void source(Map<String,QuestionImportPackage.Evidence> files,Set<String> used,String path,String hash){
        var f=files.get(path);require(path.startsWith("sources/") && f!=null && f.sha256().equals(hash));used.add(path);
    }
    private interface Write {void write(DataOutputStream out)throws IOException;}
    private static String digest(Write writer)throws IOException{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){writer.write(out);}return sha(bytes.toByteArray());}
    private static void text(DataOutputStream out,String value)throws IOException{var bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}
    public static String sha(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static void require(boolean valid){if(!valid)throw new InvalidPackage();}
}

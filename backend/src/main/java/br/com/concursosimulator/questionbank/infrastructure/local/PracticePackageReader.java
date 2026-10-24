package br.com.concursosimulator.questionbank.infrastructure.local;

import br.com.concursosimulator.questionbank.application.ApplyQuestionCandidatesUseCase.Candidate;
import br.com.concursosimulator.questionbank.domain.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import java.util.*;
import static br.com.concursosimulator.questionbank.infrastructure.local.QuestionPackageReader.*;

/** Pacote autoral próprio, sem depender do inventário legado nem aprovar conteúdo. */
final class PracticePackageReader {
    private final JsonMapper json=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    Selected read(byte[] bytes,Path root,Set<String> keys) throws java.io.IOException {
        var pack=json.readValue(bytes,QuestionPackageReader.Package.class);
        if(!"lesson-practice-package/v1".equals(pack.format()) || keys.isEmpty())throw new IllegalArgumentException("Pacote/seleção inválidos");
        var catalog=new HashMap<String,String>();for(var t:pack.catalog())if(catalog.put(t.topicId(),t.disciplineId())!=null)throw new IllegalArgumentException("Assunto duplicado");
        if(catalog.size()!=Topic.values().length)throw new IllegalArgumentException("Catálogo incompleto");
        for(var t:Topic.values())if(!t.discipline().id().equals(catalog.get(t.id())))throw new IllegalArgumentException("Catálogo divergente");
        var sources=new HashMap<String,String>();
        for(var source:pack.sourceFiles())if(sources.put(source.path(),source.sha256())!=null || !LocalFiles.sha(LocalFiles.source(root,source.path())).equals(source.sha256()))throw new IllegalArgumentException("Fonte/hash divergentes");
        var inventory=new HashMap<String,InventoryItem>();
        for(var item:pack.inventory())if(inventory.put(item.key(),item)!=null || !item.editorialReviewRequired() || !"candidate".equals(item.status()) || item.legalBasisPending()!=null)throw new IllegalArgumentException("Inventário inválido");
        if(inventory.size()!=255)throw new IllegalArgumentException("Lote deve conter255 itens próprios");
        for(var t:Topic.values())for(int n=1;n<=5;n++)if(!inventory.containsKey(t.id()+"-P0"+n))throw new IllegalArgumentException("Origem fora do lote aprovado");
        if(!inventory.keySet().containsAll(keys))throw new IllegalArgumentException("Seleção fora do inventário");
        var items=new HashMap<String,Item>();
        for(var node:pack.items()){
            var item=json.treeToValue(node,Item.class);
            if(item==null || item.origin()==null || item.origin().purpose()!=QuestionPurpose.PRATICA_AULA || !QuestionPurpose.PRACTICE_NAMESPACE.equals(item.origin().namespace())
                || !inventory.containsKey(item.origin().key()) || items.put(item.origin().key(),item)!=null)throw new IllegalArgumentException("Origem inválida/duplicada");
        }
        if(!items.keySet().equals(inventory.keySet()))throw new IllegalArgumentException("Conteúdo incompleto");
        var selected=new ArrayList<Candidate>();
        for(var key:keys.stream().sorted().toList()){
            var item=items.get(key);var c=item.content().domain();var provenance=c.provenance();
            if(!key.startsWith(c.classification().topic().id()+"-P") || !c.fingerprint().equals(item.fingerprint()))throw new IllegalArgumentException("Classificação/fingerprint divergentes");
            var path=provenance.contentPath();
            if(!path.startsWith("editorial/lesson-practice/") || !path.equals(provenance.answerPath())
                || !Objects.equals(sources.get(path),provenance.contentSha256()) || !Objects.equals(sources.get(path),provenance.answerSha256()))throw new IllegalArgumentException("Proveniência divergente");
            var expectedRoles=c.classification().discipline()==Discipline.ADMINISTRATIVE_KNOWLEDGE?Set.of(TargetRole.ADMINISTRATIVE_ASSISTANT):c.classification().discipline()==Discipline.SCHOOL_KNOWLEDGE?Set.of(TargetRole.SCHOOL_SECRETARY):Set.of(TargetRole.values());
            if(!c.classification().roles().equals(expectedRoles) || c.alternatives().size()!=4)throw new IllegalArgumentException("Cargos/alternativas divergentes");
            if(pack.issues().stream().anyMatch(i->key.equals(i.key())))throw new IllegalArgumentException("Item com pendência");
            PackageContentSafety.check(root,c);selected.add(new Candidate(item.origin(),c,item.expectedLatestRevision()));
        }
        return new Selected(List.copyOf(selected),pack.inventory(),pack.issues(),pack.possibleDuplicates());
    }
}

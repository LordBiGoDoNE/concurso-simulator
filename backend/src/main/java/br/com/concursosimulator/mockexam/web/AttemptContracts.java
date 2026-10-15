package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import java.time.Instant;
import java.util.*;

public final class AttemptContracts {
    private AttemptContracts() {}
    public record PortionRequest(String disciplineId,Integer quantity,Set<String> topicIds) {}
    public record CreateRequest(String commandId,String roleId,List<PortionRequest> disciplines) {
        Composition composition() {
            if (disciplines==null || disciplines.isEmpty() || disciplines.size()>Discipline.values().length) throw invalid();
            var map=new EnumMap<Discipline,Composition.Portion>(Discipline.class);
            for (var p:disciplines) {
                if (p==null || p.quantity()==null) throw invalid();
                var topics=p.topicIds()==null?Set.<Topic>of():p.topicIds().stream().map(Topic::fromId).collect(java.util.stream.Collectors.toSet());
                if (map.put(Discipline.fromId(p.disciplineId()),new Composition.Portion(p.quantity(),topics))!=null) throw invalid();
            }
            return new Composition(TargetRole.fromId(roleId),map);
        }
    }
    public record AnswerRequest(String commandId,Long expectedVersion,@com.fasterxml.jackson.annotation.JsonProperty(required=true) String alternativeId) {}
    public record FinishRequest(String commandId,Long expectedVersion) {}
    public record Resource(String path,String mediaType,String sha256,String description) {}
    public record Text(String text,List<Resource> resources) {
        static Text from(TextContent c) { return new Text(c.text(),c.resources().stream().map(r -> new Resource(r.path(),r.mediaType(),r.sha256(),r.description())).toList()); }
    }
    public record Classification(String disciplineId,String topicId,List<String> roleIds) {
        static Classification from(QuestionClassification c) { return new Classification(c.discipline().id(),c.topic().id(),c.roles().stream().map(TargetRole::id).sorted().toList()); }
    }
    public record Alternative(UUID id,Text content) {}
    public record Reference(String name,String url) {}
    public record Item(UUID id,UUID questionId,UUID revisionId,Classification classification,Text statement,
                       List<Alternative> alternatives,List<Reference> references,UUID selectedAlternativeId) {
        static Item from(Attempt.ItemView i) {
            return new Item(i.id(),i.questionId(),i.revisionId(),Classification.from(i.classification()),Text.from(i.statement()),
                    i.alternatives().stream().map(a -> new Alternative(a.id(),Text.from(a.content()))).toList(),
                    i.references().stream().map(r -> new Reference(r.name(),r.url().toString())).toList(),i.selectedAlternativeId());
        }
    }
    public record View(UUID id,String roleId,Instant createdAt,Instant finishedAt,long version,List<Item> items) {
        static View from(Attempt.View v) { return new View(v.id(),v.role().id(),v.createdAt(),v.finishedAt(),v.version(),v.items().stream().map(Item::from).toList()); }
    }
    public record Correction(Item item,UUID correctAlternativeId,Text resolution) {}
    public record Result(int total,int points,int correct,int wrong,int blank,java.math.BigDecimal percentage,List<Correction> items) {
        static Result from(Attempt.Result r) { return new Result(r.total(),r.correct(),r.correct(),r.wrong(),r.blank(),r.percentage(),
                r.items().stream().map(i -> new Correction(Item.from(i.item()),i.correctAlternativeId(),Text.from(i.resolution()))).toList()); }
    }
    public record Finished(View attempt,Result result) {}
    public record HistoryItem(UUID id,String roleId,Instant createdAt,Instant finishedAt,int total,java.math.BigDecimal percentage) {}
    public record History(List<HistoryItem> items,int page,int size) {}
    public record Error(String error) {}
    public record Available(String disciplineId,int available) {}
    public record Insufficient(String error,List<Available> available) {}
    static UUID uuid(String s) {
        if (s==null || !s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw invalid();
        return UUID.fromString(s);
    }
    static long version(Long v) { if (v==null || v<0) throw invalid(); return v; }
    static IllegalArgumentException invalid() { return new IllegalArgumentException("Pedido inválido"); }
}

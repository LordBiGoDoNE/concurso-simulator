package br.com.concursosimulator.questionbank.web;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import java.util.*;

public final class QuestionResponses {
    private QuestionResponses() {}
    public record Named(String id,String title) {}
    public record TopicItem(String id,String disciplineId,String title) {}
    public record Catalog(String contestId,List<Named> disciplines,List<TopicItem> topics,List<Named> roles) {}
    public record Classification(String disciplineId,String topicId,List<String> roleIds) {
        static Classification from(QuestionClassification c) { return new Classification(c.discipline().id(),c.topic().id(),c.roles().stream().map(TargetRole::id).sorted().toList()); }
    }
    public record Resource(String path,String mediaType,String sha256,String description) {}
    public record Text(String text,List<Resource> resources) {
        static Text from(TextContent text) { return new Text(text.text(),text.resources().stream().map(r -> new Resource(r.path(),r.mediaType(),r.sha256(),r.description())).toList()); }
    }
    public record Alternative(UUID id,Text content) {}
    public record Reference(String name,String url) {}
    public record Summary(UUID questionId,UUID revisionId,Classification classification,String statementStart) {}
    public record Page(List<Summary> items,int page,int size,long total) {
        static Page from(EligibleQuestions.Page page) { return new Page(page.items().stream().map(i -> new Summary(i.questionId(),i.revisionId(),Classification.from(i.classification()),i.statementStart())).toList(),page.page(),page.size(),page.total()); }
    }
    public record Detail(UUID questionId,UUID revisionId,Classification classification,Text statement,List<Alternative> alternatives,List<Reference> references) {
        static Detail from(EligibleQuestions.Detail d) { return new Detail(d.questionId(),d.revisionId(),Classification.from(d.classification()),Text.from(d.statement()),
                d.alternatives().stream().map(a -> new Alternative(a.id(),Text.from(a.content()))).toList(),d.references().stream().map(r -> new Reference(r.name(),r.url().toString())).toList()); }
    }
    public record Error(String error) {}
}

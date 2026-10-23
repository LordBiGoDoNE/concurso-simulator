package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.QuestionInventory;
import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;

/** Cobertura conta identidades, não versões nem associações de cargo. Relatório é privado/local. */
public final class QuestionEditorialReportUseCase {
    private final QuestionInventory inventory;
    public QuestionEditorialReportUseCase(QuestionInventory inventory) { this.inventory = Objects.requireNonNull(inventory); }
    public record Coverage(String disciplineId, String topicId, int originalInventory, long imported, long pending,
                           long inReview, long eligible, long withdrawn) {}
    public record Report(List<QuestionInventory.Entry> questions, List<Coverage> topics, List<Coverage> disciplines) {}
    public Report execute() {
        var entries = inventory.read(); var topics = new ArrayList<Coverage>(); var disciplines = new ArrayList<Coverage>();
        for (var topic : Topic.values()) topics.add(count(entries, topic.discipline().id(), topic.id(), 10));
        for (var discipline : Discipline.values()) disciplines.add(count(entries, discipline.id(), null,
                (int) Arrays.stream(Topic.values()).filter(t -> t.discipline()==discipline).count() * 10));
        return new Report(List.copyOf(entries), List.copyOf(topics), List.copyOf(disciplines));
    }
    private static Coverage count(List<QuestionInventory.Entry> entries, String discipline, String topic, int original) {
        var latest = entries.stream().filter(e -> matches(e.latest(),discipline,topic)).toList();
        long eligible = entries.stream().filter(e -> e.revisions().stream().anyMatch(r -> r.id().equals(e.eligibleRevisionId()) && matches(r,discipline,topic))).count();
        return new Coverage(discipline,topic,original,latest.size(),
                latest.stream().filter(e -> !e.latest().id().equals(e.eligibleRevisionId()) && e.latest().status()!=Question.Status.APPROVED).count(),
                latest.stream().filter(e -> e.latest().status()==Question.Status.IN_REVIEW).count(),eligible,
                latest.stream().filter(e -> e.eligibleRevisionId()==null && e.revisions().stream().anyMatch(r -> r.status()==Question.Status.APPROVED)).count());
    }
    private static boolean matches(QuestionInventory.Revision revision, String discipline, String topic) {
        return revision.classification().discipline().id().equals(discipline) && (topic==null || revision.classification().topic().id().equals(topic));
    }
}

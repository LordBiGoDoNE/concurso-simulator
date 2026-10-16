package br.com.concursosimulator.subjectreview.domain;

import br.com.concursosimulator.mockexam.domain.CompletedAttempt;
import br.com.concursosimulator.questionbank.domain.*;
import java.math.*;
import java.time.Instant;
import java.util.*;

/** Política subject-review/v1 sobre observações já concluídas, sem I/O ou regeneração. */
public final class ReviewSummary {
    public static final String POLICY="subject-review/v1";
    public static final int WINDOW_LIMIT=100, MIN_BASES=3;
    public record Metric(Topic topic, int correct, int wrong, int blank, int exposures, Instant lastFinishedAt) {
        public Metric {
            Objects.requireNonNull(topic);
            int assessed=Math.addExact(Math.addExact(correct,wrong),blank);
            if (correct<0 || wrong<0 || blank<0 || exposures<assessed || (assessed==0)!=(lastFinishedAt==null))
                throw new IllegalArgumentException("Métrica inconsistente");
        }
        public int assessed() { return correct+wrong+blank; }
        public int missed() { return wrong+blank; }
        public BigDecimal percentage() {
            return assessed()==0?null:BigDecimal.valueOf(correct*100L).divide(BigDecimal.valueOf(assessed()),2,RoundingMode.HALF_UP);
        }
    }
    private record Observation(CompletedAttempt attempt, CompletedAttempt.Item item) {}
    private final List<Metric> subjects;
    private final List<Topic> priorities;
    private ReviewSummary(List<Metric> subjects) {
        this.subjects=List.copyOf(subjects);
        priorities=subjects.stream().filter(m -> m.assessed()>=MIN_BASES && m.missed()>0)
                .sorted((a,b) -> {
                    int ratio=Long.compare((long)b.missed()*a.assessed(),(long)a.missed()*b.assessed());
                    if (ratio!=0) return ratio;
                    int missed=Integer.compare(b.missed(),a.missed());
                    return missed!=0?missed:a.topic().id().compareTo(b.topic().id());
                }).map(Metric::topic).toList();
    }
    public List<Metric> subjects() { return subjects; }
    public List<Topic> priorities() { return priorities; }
    public static ReviewSummary calculate(TargetRole role, List<CompletedAttempt> attempts) {
        Objects.requireNonNull(role); attempts=List.copyOf(attempts);
        if (attempts.size()>WINDOW_LIMIT) throw new IllegalArgumentException("Janela excedida");
        var ids=new HashSet<UUID>();
        var latest=new HashMap<UUID,Observation>();
        var exposures=new EnumMap<Topic,Integer>(Topic.class);
        var owner=attempts.isEmpty()?null:attempts.getFirst().owner();
        for (var attempt:attempts) {
            if (!ids.add(attempt.id()) || attempt.role()!=role || !attempt.owner().equals(owner))
                throw new IllegalArgumentException("Janela mistura identidades, cargos ou tentativas");
            for (var item:attempt.items()) {
                exposures.merge(item.classification().topic(),1,Math::addExact);
                latest.merge(item.questionId(),new Observation(attempt,item),
                        (a,b) -> CompletedAttempt.ORDER.compare(a.attempt(),b.attempt())>0?a:b);
            }
        }
        var counts=new EnumMap<Topic,int[]>(Topic.class);
        var dates=new EnumMap<Topic,Instant>(Topic.class);
        for (var observation:latest.values()) {
            var topic=observation.item().classification().topic();
            counts.computeIfAbsent(topic,t -> new int[3])[observation.item().outcome().ordinal()]++;
            dates.merge(topic,observation.attempt().finishedAt(),(a,b) -> a.isAfter(b)?a:b);
        }
        var result=new ArrayList<Metric>();
        for (var topic:Topic.values()) {
            var c=counts.getOrDefault(topic,new int[3]);
            result.add(new Metric(topic,c[0],c[1],c[2],exposures.getOrDefault(topic,0),dates.get(topic)));
        }
        return new ReviewSummary(result);
    }
}

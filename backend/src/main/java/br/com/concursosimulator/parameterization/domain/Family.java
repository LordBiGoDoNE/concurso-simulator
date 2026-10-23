package br.com.concursosimulator.parameterization.domain;

import br.com.concursosimulator.questionbank.domain.Topic;
import java.util.*;

/** Domínios fechados da versão numeric-v1, não uma linguagem de fórmulas. */
public enum Family {
    DIRECT_PROPORTION(Topic.M03, Map.of("a", List.of(2,3,4,5), "b", List.of(6,7,8,9), "u", List.of(2,3,5,7))),
    PERCENTAGE(Topic.M04, Map.of("v", List.of(100,200,400,800), "p", List.of(5,10,15,20,25))),
    SIMPLE_INTEREST(Topic.M04, Map.of("c", List.of(1000,2000,4000), "i", List.of(1,2,3), "t", List.of(2,3,6)));

    private final Topic topic;
    private final SortedMap<String,List<Integer>> ranges;
    Family(Topic topic, Map<String,List<Integer>> ranges) {
        this.topic=topic; this.ranges=Collections.unmodifiableSortedMap(new TreeMap<>(ranges));
    }
    public Topic topic() { return topic; }
    public void validate(Map<String,Integer> values) {
        Objects.requireNonNull(values);
        if (!values.keySet().equals(ranges.keySet()) || ranges.entrySet().stream()
                .anyMatch(e -> !e.getValue().contains(values.get(e.getKey()))))
            throw new IllegalArgumentException("Parâmetros fora do domínio fechado");
    }
    public List<Parameters> domain() {
        var result=new ArrayList<Parameters>(); enumerate(new ArrayList<>(ranges.keySet()),0,new TreeMap<>(),result);
        return List.copyOf(result);
    }
    private void enumerate(List<String> keys,int position,Map<String,Integer> values,List<Parameters> result) {
        if (position==keys.size()) { result.add(new Parameters(this,values)); return; }
        var key=keys.get(position);
        for (var value:ranges.get(key)) { values.put(key,value); enumerate(keys,position+1,values,result); }
    }
    public String domainDigest() {
        var fields=new ArrayList<String>(List.of("parameter-domain/v1",name()));
        domain().forEach(p -> fields.add(p.key())); return CanonicalDigest.of(fields);
    }
}

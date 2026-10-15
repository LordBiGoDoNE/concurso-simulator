package br.com.concursosimulator.parameterization.domain;

import java.util.*;

/** Mapa interno de schema fechado; nunca é um contrato HTTP nem recebe valores do aluno. */
public record Parameters(Family family,Map<String,Integer> values) {
    public Parameters {
        Objects.requireNonNull(family); family.validate(values);
        values=Collections.unmodifiableSortedMap(new TreeMap<>(values));
    }
    public int value(String key) {
        var value=values.get(key); if(value==null) throw new IllegalArgumentException("Parâmetro desconhecido");
        return value;
    }
    public String key() {
        return family.name()+":"+values.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).toList();
    }
}

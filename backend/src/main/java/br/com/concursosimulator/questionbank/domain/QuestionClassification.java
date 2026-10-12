package br.com.concursosimulator.questionbank.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Uma classificação por conteúdo de revisão, com cargos associados sem duplicação. */
public record QuestionClassification(Discipline discipline, Topic topic, Set<TargetRole> roles) {
    public QuestionClassification {
        if (discipline == null || topic == null || topic.discipline() != discipline) {
            throw new IllegalArgumentException("Assunto não pertence à disciplina informada");
        }
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(role -> role == null)) {
            throw new IllegalArgumentException("Classificação exige cargos válidos de Limeira 01/2026");
        }
        roles = Set.copyOf(roles);
    }

    public static QuestionClassification fromIds(String disciplineId, String topicId, Collection<String> roleIds) {
        if (roleIds == null) throw new IllegalArgumentException("Cargos obrigatórios");
        var roles = new HashSet<TargetRole>();
        for (var id : roleIds) roles.add(TargetRole.fromId(id));
        return new QuestionClassification(Discipline.fromId(disciplineId), Topic.fromId(topicId), roles);
    }

    public boolean appliesTo(TargetRole role) { return role != null && roles.contains(role); }
}

package br.com.concursosimulator.questionbank.domain;

/** Namespace impede colisão de chaves de fontes distintas. Não contém identidade Google. */
public record QuestionOrigin(String namespace, String key) {
    public QuestionOrigin {
        ContentRules.text(namespace, 100, "Namespace");
        ContentRules.text(key, 200, "Chave de origem");
    }
}

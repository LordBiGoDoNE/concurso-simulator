package br.com.concursosimulator.questionbank.domain;

/** A coleção é parte da origem imutável, não uma escolha de quem monta a tentativa. */
public enum QuestionPurpose {
    SIMULADO, PRATICA_AULA;
    public static final String PRACTICE_NAMESPACE = "lesson-practice-v1";
    public static QuestionPurpose forOrigin(String namespace) {
        return PRACTICE_NAMESPACE.equals(namespace) ? PRATICA_AULA : SIMULADO;
    }
}

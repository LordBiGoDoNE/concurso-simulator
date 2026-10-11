package br.com.concursosimulator.questionbank.domain;

public enum Discipline {
    PORTUGUESE("portugues", "Português"),
    MATHEMATICS_LOGIC("matematica-raciocinio", "Matemática/Raciocínio Lógico"),
    COMPUTING("informatica", "Informática"),
    ADMINISTRATIVE_KNOWLEDGE("conhecimentos-administrativos", "Conhecimentos Administrativos"),
    SCHOOL_KNOWLEDGE("conhecimentos-escolares", "Conhecimentos Escolares");

    private final String id;
    private final String title;

    Discipline(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() { return id; }
    public String title() { return title; }

    public static Discipline fromId(String id) {
        for (var discipline : values()) {
            if (discipline.id.equals(id)) return discipline;
        }
        throw new IllegalArgumentException("Disciplina fora do catálogo: " + id);
    }
}

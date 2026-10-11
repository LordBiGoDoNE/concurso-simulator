package br.com.concursosimulator.questionbank.domain;

/** Cargos de estudo, não permissões de autenticação ou de operação editorial. */
public enum TargetRole {
    ADMINISTRATIVE_ASSISTANT("assistente-administrativo", "Assistente Administrativo"),
    SCHOOL_SECRETARY("secretario-de-escola", "Secretário de Escola");

    public static final String CONTEST_ID = "limeira-01-2026";

    private final String id;
    private final String title;

    TargetRole(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() { return id; }
    public String title() { return title; }

    public static TargetRole fromId(String id) {
        for (var role : values()) {
            if (role.id.equals(id)) return role;
        }
        throw new IllegalArgumentException("Cargo fora de Limeira 01/2026: " + id);
    }
}

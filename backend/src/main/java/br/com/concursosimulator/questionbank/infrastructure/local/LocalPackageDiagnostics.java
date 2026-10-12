package br.com.concursosimulator.questionbank.infrastructure.local;

import java.util.List;

/** Somente IDs/códigos fixos; nunca ecoar mensagens/payloads de pacote, Jackson ou JDBC. */
final class LocalPackageDiagnostics {
    record Diagnostic(String key,String code) {}
    static final class InvalidSelection extends IllegalArgumentException {
        private final List<Diagnostic> diagnostics;
        InvalidSelection(List<Diagnostic> diagnostics) { super("Item selecionado inválido");this.diagnostics=List.copyOf(diagnostics); }
        List<Diagnostic> diagnostics() { return diagnostics; }
    }
    static String key(String key) { return key!=null && key.matches("[PMIAS][0-9]{2}-Q[0-9]{2}")?key:"INVALID_KEY"; }
}

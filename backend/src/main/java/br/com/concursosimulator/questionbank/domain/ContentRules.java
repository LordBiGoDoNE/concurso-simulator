package br.com.concursosimulator.questionbank.domain;

final class ContentRules {
    private ContentRules() {}
    static String text(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.length() > maximum
                || value.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
            throw new IllegalArgumentException(field + " inválido");
        }
        return value; // Não fazer trim/normalização lossy de Unicode ou fórmulas.
    }
    static String hash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("SHA-256 inválido");
        return value;
    }
    static String localPath(String value) {
        text(value, 1000, "Caminho local");
        if (value.startsWith("/") || value.contains("\\") || value.contains(":") || value.contains("%")) {
            throw new IllegalArgumentException("Caminho deve ser local e relativo");
        }
        for (var segment : value.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) throw new IllegalArgumentException("Caminho inválido");
        }
        return value;
    }
}

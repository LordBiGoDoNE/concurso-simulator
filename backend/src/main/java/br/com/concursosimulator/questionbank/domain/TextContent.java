package br.com.concursosimulator.questionbank.domain;

import java.util.List;

/** Texto acessível (inclusive fórmulas por extenso/Unicode), nunca HTML executável. */
public record TextContent(String text, List<Resource> resources) {
    public TextContent {
        ContentRules.text(text, 100_000, "Texto acessível");
        resources = List.copyOf(resources);
    }
    public TextContent(String text) { this(text, List.of()); }

    public record Resource(String path, String mediaType, String sha256, String description) {
        public Resource {
            ContentRules.localPath(path);
            if (!List.of("image/png", "image/jpeg", "image/svg+xml").contains(mediaType)) {
                throw new IllegalArgumentException("Tipo de recurso não permitido");
            }
            ContentRules.hash(sha256);
            ContentRules.text(description, 4000, "Descrição acessível do recurso");
        }
    }
}

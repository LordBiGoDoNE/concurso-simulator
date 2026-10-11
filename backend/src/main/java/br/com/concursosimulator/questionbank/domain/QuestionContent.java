package br.com.concursosimulator.questionbank.domain;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record QuestionContent(TextContent statement, List<Alternative> alternatives, UUID correctAlternativeId,
                              TextContent resolution, QuestionClassification classification,
                              List<StudyReference> references, Provenance provenance) {
    public QuestionContent {
        Objects.requireNonNull(statement); Objects.requireNonNull(resolution);
        Objects.requireNonNull(classification); Objects.requireNonNull(provenance);
        alternatives = List.copyOf(alternatives); references = List.copyOf(references);
        if (alternatives.size() < 2 || references.isEmpty()) throw new IllegalArgumentException("Alternativas/fontes insuficientes");
        var ids = new HashSet<UUID>(); var texts = new HashSet<TextContent>();
        for (var option : alternatives) {
            if (!ids.add(option.id()) || !texts.add(option.content())) throw new IllegalArgumentException("Alternativas repetidas");
        }
        if (!ids.contains(correctAlternativeId)) throw new IllegalArgumentException("Resposta não pertence à revisão");
    }
    public record Alternative(UUID id, TextContent content) {
        public Alternative { Objects.requireNonNull(id); Objects.requireNonNull(content); }
    }

    /** Canonização binária v1: UTF-8 com comprimento; listas editoriais ordenadas, cargos por ID. */
    public String fingerprint() {
        try {
            var bytes = new ByteArrayOutputStream(); var out = new DataOutputStream(bytes);
            write(out, "question-content/v1"); writeContent(out, statement);
            out.writeInt(alternatives.size());
            for (var option : alternatives) { write(out, option.id().toString()); writeContent(out, option.content()); }
            write(out, correctAlternativeId.toString()); writeContent(out, resolution);
            write(out, classification.discipline().id()); write(out, classification.topic().id());
            var roles = classification.roles().stream().map(TargetRole::id).sorted().toList();
            out.writeInt(roles.size()); for (var role : roles) write(out, role);
            out.writeInt(references.size());
            for (var reference : references) {
                write(out, reference.name()); write(out, reference.url().toString());
                out.writeBoolean(reference.legalBasis() != null);
                if (reference.legalBasis() != null) {
                    write(out, reference.legalBasis().provision()); write(out, reference.legalBasis().checkedOn().toString());
                }
            }
            write(out, provenance.author()); write(out, provenance.contentPath()); write(out, provenance.answerPath());
            write(out, provenance.contentSha256()); write(out, provenance.answerSha256()); out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void writeContent(DataOutputStream out, TextContent content) throws IOException {
        write(out, content.text()); out.writeInt(content.resources().size());
        for (var resource : content.resources()) {
            write(out, resource.path()); write(out, resource.mediaType()); write(out, resource.sha256()); write(out, resource.description());
        }
    }
    private static void write(DataOutputStream out, String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8); out.writeInt(bytes.length); out.write(bytes);
    }
}

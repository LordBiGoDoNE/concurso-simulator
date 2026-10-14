package br.com.concursosimulator.questionbank.infrastructure.local;

import java.nio.file.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/** Raiz controlada e imóvel pelo operador; não resolve symlinks nem lê fora das raízes aprovadas. */
final class LocalFiles {
    private static final Set<String> ROOTS = Set.of("01_Portugues", "02_Matematica_Logica", "03_Informatica",
            "04_Assistente_Administrativo", "05_Secretario_Escola", "assets");
    static byte[] source(Path root, String relative) throws IOException {
        if (relative == null || relative.isEmpty() || relative.contains("\\") || relative.startsWith("/")) throw new IllegalArgumentException("Caminho inválido");
        var parts = relative.split("/", -1);
        for (var part : parts) if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IllegalArgumentException("Traversal/caminho não canônico");
        if (!ROOTS.contains(parts[0]) && !Set.of("manifesto_v3.json", "catalogo_fontes_questoes.csv").contains(relative))
            throw new IllegalArgumentException("Raiz de fonte não permitida");
        var path = root.toRealPath();
        for (var part : parts) { path = path.resolve(part); if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("Symlink recusado"); }
        return bounded(path, 16_000_000);
    }
    static byte[] bounded(Path path, int limit) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > limit) throw new IllegalArgumentException("Arquivo ausente/acima do limite");
        try (var stream = Files.newInputStream(path)) {
            var data = stream.readNBytes(limit + 1);
            if (data.length > limit) throw new IllegalArgumentException("Arquivo acima do limite");
            return data;
        }
    }
    static String sha(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}

package br.com.concursosimulator.parameterization.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Lista ordenada, tamanho e campos UTF-8 prefixados pelo comprimento; sem JSON dependente de mapper. */
final class CanonicalDigest {
    private CanonicalDigest() {}
    static String of(List<String> fields) {
        try {
            var bytes=new ByteArrayOutputStream(); var out=new DataOutputStream(bytes);
            out.writeInt(fields.size());
            for(var field:fields) { var encoded=Objects.requireNonNull(field).getBytes(StandardCharsets.UTF_8); out.writeInt(encoded.length); out.write(encoded); }
            out.flush(); return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException | NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static String hash(String value) {
        if(value==null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Digest inválido");
        return value;
    }
}

package br.com.concursosimulator.parameterization.infrastructure.local;

import java.nio.file.*;
import java.io.IOException;
import java.security.*;
import java.util.*;

/** Raiz escolhida pelo operador deve permanecer estável durante a confirmação. Sem seguir symlinks internos. */
final class ControlledInput {
    private ControlledInput() {}
    static byte[] read(Path root,String relative) throws IOException {
        if(relative==null || relative.isBlank() || relative.startsWith("/") || relative.contains("\\") || relative.contains(":") || relative.contains("%"))
            throw new IllegalArgumentException("Arquivo relativo não canônico");
        var path=root.toRealPath();
        for(var part:relative.split("/",-1)) {
            if(part.isBlank() || part.equals(".") || part.equals(".."))throw new IllegalArgumentException("Traversal recusado");
            path=path.resolve(part);if(Files.isSymbolicLink(path))throw new IllegalArgumentException("Symlink recusado");
        }
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) || Files.size(path)>65536)throw new IllegalArgumentException("Arquivo ausente/acima do limite");
        try(var input=Files.newInputStream(path)) {
            var bytes=input.readNBytes(65537);if(bytes.length>65536)throw new IllegalArgumentException("Arquivo acima do limite");return bytes;
        }
    }
    static String sha(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    static String destination(String url) {
        if(url==null || !url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException("DB_URL explícita obrigatória");
        var uri=java.net.URI.create(url.substring(5));
        if(!Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()) || uri.getUserInfo()!=null || uri.getFragment()!=null
                || uri.getPath()==null || !uri.getPath().matches("/[a-zA-Z0-9_]+") || uri.getPort() < -1 || uri.getPort()==0)
            throw new IllegalArgumentException("Destino PostgreSQL local obrigatório");
        if(uri.getQuery()!=null)for(var part:uri.getQuery().split("&",-1))
            if(!part.matches("(currentSchema=[a-zA-Z0-9_]+|connectTimeout=[0-9]+|socketTimeout=[0-9]+|logServerErrorDetail=false)"))throw new IllegalArgumentException("Parâmetro JDBC recusado");
        return url;
    }
}

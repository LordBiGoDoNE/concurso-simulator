package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.QuestionImportConfirmations;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Envelope de confirmação, separado de autenticação e CSRF. Não persiste a prévia. */
public final class ImportConfirmationTokens implements QuestionImportConfirmations {
    private final byte[] key;private final Clock clock;
    public ImportConfirmationTokens(byte[] key,Clock clock){this.key=key.clone();this.clock=clock;}
    public Issued issue(UUID actor,String destination,QuestionImportPackage pack){
        var now=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var c=new ImportConfirmation(UUID.randomUUID(),actor,destination,pack.digest(),pack.selectionDigest(),now,now.plusSeconds(900));
        try {
            var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){
                out.writeInt(1);out.writeUTF(c.nonce().toString());out.writeUTF(c.actor().toString());out.writeUTF(c.destination());
                out.writeUTF(c.packageDigest());out.writeUTF(c.selectionDigest());out.writeLong(now.getEpochSecond());out.writeLong(c.expiresAt().getEpochSecond());
            }
            byte[] payload=bytes.toByteArray();return new Issued(encode(payload)+"."+encode(mac(payload)),c.expiresAt());
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public ImportConfirmation verify(String token){
        try {
            if(token==null || token.length()>4096)throw new IllegalArgumentException();
            var parts=token.split("\\.",-1);if(parts.length!=2)throw new IllegalArgumentException();
            var payload=Base64.getUrlDecoder().decode(parts[0]);var signature=Base64.getUrlDecoder().decode(parts[1]);
            if(!encode(payload).equals(parts[0]) || !encode(signature).equals(parts[1]) || !MessageDigest.isEqual(mac(payload),signature))throw new IllegalArgumentException();
            try(var in=new DataInputStream(new ByteArrayInputStream(payload))){
                if(in.readInt()!=1)throw new IllegalArgumentException();
                var c=new ImportConfirmation(UUID.fromString(in.readUTF()),UUID.fromString(in.readUTF()),in.readUTF(),in.readUTF(),in.readUTF(),Instant.ofEpochSecond(in.readLong()),Instant.ofEpochSecond(in.readLong()));
                if(in.available()!=0)throw new IllegalArgumentException();return c;
            }
        }catch(IllegalArgumentException | IOException error){throw new ApplyAdministrativeQuestionsUseCase.ConfirmationFailure(false);}
    }
    private byte[] mac(byte[] payload){
        if(key.length<32)throw new IllegalStateException("Importação desabilitada");
        try {var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(payload);}
        catch(GeneralSecurityException error){throw new IllegalStateException(error);}
    }
    private static String encode(byte[] bytes){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
}

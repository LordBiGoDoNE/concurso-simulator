package br.com.concursosimulator.questionbank.infrastructure.importing;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ImportSettings {
    private final String destination;private final byte[] secret;
    public ImportSettings(@Value("${app.question-import.allowed-user-ids:}")String legacy,
            @Value("${app.question-import.destination-id:}")String destination,
            @Value("${app.question-import.confirmation-key:}")String key) {
        try {
            if(!legacy.isBlank())throw new IllegalArgumentException();
            this.destination=destination;secret=key.isEmpty()?new byte[0]:Base64.getDecoder().decode(key);
            if((!destination.isEmpty() || secret.length!=0) && (!destination.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}") || secret.length<32 || secret.length>128))throw new IllegalArgumentException();
        }catch(IllegalArgumentException error){throw new IllegalArgumentException("Configuração administrativa inválida");}
    }
    public boolean configured(){return !destination.isEmpty() && secret.length>=32;}
    public String destination(){return destination;}
    byte[] secret(){return secret.clone();}
}

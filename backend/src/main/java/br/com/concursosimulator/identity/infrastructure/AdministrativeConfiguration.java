package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.application.port.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
public class AdministrativeConfiguration {
    @Bean AdministrativePasswords administrativePasswords() {
        var encoder=new Pbkdf2PasswordEncoder("",16,600000,256);
        encoder.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return new AdministrativePasswords() {
            private static final String PREFIX="{pbkdf2-sha256-600k}";
            public String encode(String value) { return PREFIX+encoder.encode(value); }
            public boolean matches(String value,String hash) {
                return hash!=null && hash.startsWith(PREFIX) && encoder.matches(value,hash.substring(PREFIX.length()));
            }
        };
    }
    @Bean AdministrativeAccessUseCases administrativeAccess(AdministrativeAccounts accounts,AdministrativePasswords passwords) {
        return new AdministrativeAccessUseCases(accounts,passwords,Clock.systemUTC());
    }
    @Bean ApplicationRunner administrativeBootstrap(AdministrativeAccessUseCases access,
            @Value("${app.admin.bootstrap.username:admin}")String username,
            @Value("${app.admin.bootstrap.password:}")String configured) {
        return args -> {
            // The local recovery executable excludes this configuration and never bootstraps a new account.
            boolean generated=configured.isEmpty();
            String initial=generated?temporaryPassword():configured;
            if(access.bootstrap(username,initial,generated) && generated)
                System.out.println("ADM initial temporary password (private operator console; change after login): "+initial);
        };
    }
    public static String temporaryPassword() {
        var bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.infrastructure.persistence.*;
import java.io.*;
import java.net.URI;
import java.time.Clock;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;

/** Deliberate local recovery executable. No component/config stereotype and no web bootstrap. */
public final class AdministrativeRecovery {
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses=UserPersistenceEntity.class)
    @Import(JpaAdministrativeAccounts.class)
    public static class PersistenceOnly {}
    public static void main(String[] args) {
        int code=launch(args,System.getenv(),new BufferedReader(new InputStreamReader(System.in)),System.out,System.err);
        if(code!=0)System.exit(code);
    }
    public static int launch(String[] args,Map<String,String> env,BufferedReader input,PrintStream output,PrintStream errors) {
        try {
            if(args.length!=1 || !"--recover-admin".equals(args[0]))throw new IllegalArgumentException();
            String url=destination(env.get("DB_URL"));
            if(env.get("DB_USERNAME")==null || env.get("DB_PASSWORD")==null)throw new IllegalArgumentException();
            output.println("Local password recovery. Accounts, Google and history will be preserved. Destination: "+url);
            output.println("Type exactly: RESET ADM "+url);output.flush();
            if(!("RESET ADM "+url).equals(input.readLine())){output.println("Cancelled; no database access.");return 2;}
            var app=new SpringApplication(PersistenceOnly.class);app.setWebApplicationType(WebApplicationType.NONE);
            app.setEnvironment(new org.springframework.core.env.StandardEnvironment(){
                @Override protected void customizePropertySources(org.springframework.core.env.MutablePropertySources sources) {
                    sources.addLast(new org.springframework.core.env.MapPropertySource("recovery-only",Map.of("DB_URL",url,"DB_USERNAME",env.get("DB_USERNAME"),"DB_PASSWORD",env.get("DB_PASSWORD"))));
                }
            });
            String temporary=AdministrativeConfiguration.temporaryPassword();
            try(var context=app.run("--spring.config.location=classpath:/application.yaml","--spring.flyway.enabled=false",
                    "--spring.jpa.hibernate.ddl-auto=validate","--spring.jpa.open-in-view=false","--spring.main.web-application-type=none",
                    "--spring.main.banner-mode=off","--logging.level.root=OFF")) {
                var passwords=new AdministrativeConfiguration().administrativePasswords();
                var access=new AdministrativeAccessUseCases(context.getBean(JpaAdministrativeAccounts.class),passwords,Clock.systemUTC());
                access.recover(temporary);
            }
            output.println("Commit confirmed. ADM temporary password (private operator console): "+temporary);return 0;
        }catch(Exception failed){errors.println("Administrative recovery refused or unavailable. No success confirmed; inspect local configuration/schema.");return 1;}
    }
    static String destination(String url) {
        if(url==null || !url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException();
        var uri=URI.create(url.substring(5));
        if(!Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()) || uri.getUserInfo()!=null || uri.getFragment()!=null
                || uri.getPath()==null || !uri.getPath().matches("/[A-Za-z0-9_]+") || uri.getPort()==0)throw new IllegalArgumentException();
        if(uri.getQuery()!=null)for(var part:uri.getQuery().split("&",-1))if(!part.matches("(currentSchema=[A-Za-z0-9_]+|connectTimeout=[0-9]+|socketTimeout=[0-9]+|logServerErrorDetail=false)"))throw new IllegalArgumentException();
        return url;
    }
}

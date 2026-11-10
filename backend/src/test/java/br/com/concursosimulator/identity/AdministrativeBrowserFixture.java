package br.com.concursosimulator.identity;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Separate ports, database and test-only provider; never part of the production artifact. */
public final class AdministrativeBrowserFixture {
    public static void main(String[] args)throws Exception {
        var pg=new PostgreSQLContainer("postgres:18.6");pg.start();var oidc=new OidcFixture(19087);
        var registration=new OidcTestRegistration().fixtureRegistration(oidc.issuer(),"http://127.0.0.1:19086/login/oauth2/code/google");
        var current=new AtomicReference<ConfigurableApplicationContext>();
        try {
            current.set(start(pg,registration));
            br.com.concursosimulator.questionbank.QuestionBrowserFixtures.seed(current.get());
            current.get().getBean(AdministrativeAccessUseCases.class).recover("synthetic-browser-temporary-password");
            oidc.enableApiRestart(()->{current.get().close();current.set(start(pg,registration));});
        }catch(RuntimeException failed){oidc.close();pg.stop();throw failed;}
        Runtime.getRuntime().addShutdownHook(new Thread(()->{current.get().close();oidc.close();pg.stop();}));
        new java.util.concurrent.CountDownLatch(1).await();
    }
    static ConfigurableApplicationContext start(PostgreSQLContainer pg,ClientRegistrationRepository registration) {
        return new SpringApplicationBuilder(ConcursoSimulatorApplication.class)
            .initializers(app->app.getBeanFactory().registerSingleton("fixtureRegistration",registration))
            .run("--spring.profiles.active=local","--server.address=127.0.0.1","--server.port=19086",
                "--spring.datasource.url="+pg.getJdbcUrl(),"--spring.datasource.username="+pg.getUsername(),"--spring.datasource.password="+pg.getPassword(),
                "--spring.session.timeout=5m","--app.google.enabled=true","--app.google.callback-url=http://127.0.0.1:19086/login/oauth2/code/google",
                "--app.google.frontend-url=http://127.0.0.1:5276","--app.cors-origins=http://127.0.0.1:5276",
                "--app.admin.bootstrap.password=synthetic-initial-browser-password",
                "--app.question-import.destination-id=isolated-admin-browser",
                "--app.question-import.confirmation-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }
}

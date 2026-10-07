package br.com.concursosimulator.identity;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Launcher exclusivo de testes; nunca empacotado no bootJar. */
public final class IdentityBrowserFixture {
    public static void main(String[] args) throws Exception {
        var postgres = new PostgreSQLContainer("postgres:18.6");
        postgres.start();
        var oidc = new OidcFixture(18081);
        org.springframework.context.ConfigurableApplicationContext context;
        try {
            var registration = new OidcTestRegistration().fixtureRegistration(oidc.issuer(),
                    "http://127.0.0.1:18080/login/oauth2/code/google");
            context = new SpringApplicationBuilder(ConcursoSimulatorApplication.class)
                .initializers(application -> application.getBeanFactory().registerSingleton("fixtureRegistration", registration))
                .run("--spring.profiles.active=local", "--server.address=127.0.0.1", "--server.port=18080",
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.session.timeout=20s", "--app.google.enabled=true",
                        "--fixture.issuer=" + oidc.issuer(),
                        "--app.google.callback-url=http://127.0.0.1:18080/login/oauth2/code/google",
                        "--app.google.frontend-url=http://127.0.0.1:5173",
                        "--app.cors-origins=http://127.0.0.1:5173");
        } catch (RuntimeException exception) {
            oidc.close();
            postgres.stop();
            throw exception;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            context.close();
            oidc.close();
            postgres.stop();
        }));
        new java.util.concurrent.CountDownLatch(1).await();
    }
}

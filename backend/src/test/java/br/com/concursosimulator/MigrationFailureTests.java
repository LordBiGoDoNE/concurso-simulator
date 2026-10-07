package br.com.concursosimulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationFailureTests {
    @Test
    void failedMigrationPreventsStartup() {
        try (var postgres = new PostgreSQLContainer("postgres:18.6")) {
            postgres.start();
            assertThatThrownBy(() -> {
                try (var context = new SpringApplicationBuilder(ConcursoSimulatorApplication.class)
                        .web(WebApplicationType.SERVLET)
                        .run("--server.port=0", "--spring.datasource.url=" + postgres.getJdbcUrl(),
                                "--spring.datasource.username=" + postgres.getUsername(),
                                "--spring.datasource.password=" + postgres.getPassword(),
                                "--spring.flyway.locations=classpath:failed-migration")) {
                    throw new AssertionError("A inicialização não deveria ser concluída");
                }
            }).isInstanceOf(org.springframework.beans.BeansException.class)
                    .hasStackTraceContaining("invalid.sql");
        }
    }
}

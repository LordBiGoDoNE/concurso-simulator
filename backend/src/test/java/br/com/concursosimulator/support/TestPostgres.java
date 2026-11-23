package br.com.concursosimulator.support;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL de teste: imagem única e um servidor compartilhado por JVM de teste, com um banco vazio e
 * exclusivo para cada classe ({@link #newDatabase()}). O servidor é encerrado pelo Ryuk do Testcontainers
 * no fim da execução.
 *
 * <p>Testes que param, pausam ou reconfiguram o servidor e as fixtures que são processos {@code main}
 * mantêm contêiner próprio com {@link #IMAGE} e o envolvem com {@link TestDatabase#of(PostgreSQLContainer)}.
 */
public final class TestPostgres {
    public static final String IMAGE = "postgres:18.6";

    // Cada contexto Spring em cache mantém o próprio pool; o padrão (100) não basta para a suíte inteira.
    private static final int MAX_CONNECTIONS = 500;
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static volatile PostgreSQLContainer shared;

    private TestPostgres() {}

    /** Banco novo e vazio no servidor compartilhado. */
    public static TestDatabase newDatabase() {
        var server = server();
        var name = "t" + SEQUENCE.incrementAndGet() + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (var connection =
                        DriverManager.getConnection(server.getJdbcUrl(), server.getUsername(), server.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("create database " + name);
        } catch (SQLException failure) {
            throw new IllegalStateException("could not create test database " + name, failure);
        }
        return new TestDatabase(server, name);
    }

    static PostgreSQLContainer server() {
        var current = shared;
        if (current != null) return current;
        synchronized (TestPostgres.class) {
            if (shared == null) {
                var server = new PostgreSQLContainer(IMAGE)
                        // Senha distinta: testes verificam que ela nunca aparece na saída.
                        .withPassword("isolated-fixture-secret-not-logged")
                        .withCommand("postgres", "-c", "max_connections=" + MAX_CONNECTIONS);
                server.start();
                shared = server;
            }
            return shared;
        }
    }

    /** Banco de teste com a mesma API de leitura que os testes usavam do contêiner. */
    public static final class TestDatabase {
        private final PostgreSQLContainer server;
        private final String name;

        TestDatabase(PostgreSQLContainer server, String name) {
            this.server = server;
            this.name = name;
        }

        /** Banco padrão de um contêiner dedicado (testes de indisponibilidade e fixtures). */
        public static TestDatabase of(PostgreSQLContainer dedicated) {
            return new TestDatabase(dedicated, dedicated.getDatabaseName());
        }

        public String getJdbcUrl() {
            var url = server.getJdbcUrl();
            var current = "/" + server.getDatabaseName();
            var at = url.indexOf(current + "?");
            if (at < 0 && url.endsWith(current)) return url.substring(0, url.length() - current.length()) + "/" + name;
            if (at < 0) throw new IllegalStateException("unexpected JDBC URL format");
            return url.substring(0, at) + "/" + name + url.substring(at + current.length());
        }

        public String getUsername() {
            return server.getUsername();
        }

        public String getPassword() {
            return server.getPassword();
        }

        public String getDatabaseName() {
            return name;
        }

        public String getHost() {
            return server.getHost();
        }

        public Integer getMappedPort(int port) {
            return server.getMappedPort(port);
        }

        public ExecResult execInContainer(String... command) throws IOException, InterruptedException {
            return server.execInContainer(command);
        }
    }
}

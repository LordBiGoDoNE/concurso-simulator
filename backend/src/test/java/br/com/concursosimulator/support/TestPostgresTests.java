package br.com.concursosimulator.support;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.support.TestPostgres.TestDatabase;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class TestPostgresTests {
    @Test
    void eachCallGetsItsOwnEmptyDatabaseOnTheSharedServer() throws SQLException {
        var first = TestPostgres.newDatabase();
        var second = TestPostgres.newDatabase();

        assertThat(first.getDatabaseName()).isNotEqualTo(second.getDatabaseName());
        assertThat(first.getMappedPort(5432)).isEqualTo(second.getMappedPort(5432));
        assertThat(tables(first)).isZero();

        withJdbc(first, jdbc -> {
            jdbc.execute("create table marker(id int)");
            return jdbc.update("insert into marker values (1)");
        });

        assertThat(tables(first)).isEqualTo(1);
        assertThat(tables(second)).isZero();
    }

    @Test
    void jdbcUrlPointsToTheOwnDatabaseAndKeepsTheDriverParameters() throws SQLException {
        var database = TestPostgres.newDatabase();

        assertThat(database.getJdbcUrl()).contains("/" + database.getDatabaseName() + "?");
        try (var connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword())) {
            assertThat(connection.getCatalog()).isEqualTo(database.getDatabaseName());
        }
    }

    @Test
    void execInContainerReachesTheOwnDatabase() throws Exception {
        var database = TestPostgres.newDatabase();
        var result = database.execInContainer(
                "psql", "-U", database.getUsername(), "-d", database.getDatabaseName(), "-tAc", "select current_database()");

        assertThat(result.getExitCode()).isZero();
        assertThat(result.getStdout().trim()).isEqualTo(database.getDatabaseName());
    }

    private static int tables(TestDatabase database) throws SQLException {
        return withJdbc(database, jdbc -> jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public'", Integer.class));
    }

    private static <T> T withJdbc(TestDatabase database, Function<JdbcTemplate, T> work) throws SQLException {
        try (var connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword())) {
            return work.apply(new JdbcTemplate(new SingleConnectionDataSource(connection, true)));
        }
    }
}

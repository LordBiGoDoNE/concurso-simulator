package br.com.concursosimulator.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class IdentityStorageTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    String schema;
    DriverManagerDataSource dataSource;
    JdbcTemplate jdbc;
    GoogleIdentityService service;

    @BeforeEach
    void database() {
        schema = "identity_" + UUID.randomUUID().toString().replace("-", "");
        String url = POSTGRES.getJdbcUrl();
        dataSource = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        service = new GoogleIdentityService(jdbc, new DataSourceTransactionManager(dataSource));
    }

    Flyway flyway(String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .target(target).load();
    }

    @Test
    void emptyDatabaseMigratesAndRemainsIdempotent() {
        assertThat(flyway("2").migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway("2").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM application_marker", Integer.class)).isEqualTo(1);
        assertCounts(0);
    }

    @Test
    void upgradesV1WithoutChangingExistingData() {
        flyway("1").migrate();
        jdbc.update("UPDATE application_marker SET created_at = TIMESTAMPTZ '2000-01-01 00:00:00Z'");
        var previous = jdbc.queryForMap("SELECT * FROM application_marker");
        assertThat(flyway("2").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM application_marker")).isEqualTo(previous);
        assertCounts(0);
    }

    @Test
    void sameSubjectKeepsUuidAcrossServiceInstances() {
        flyway("2").migrate();
        var first = service.resolveVerifiedSubject("subject-one");
        var restarted = new GoogleIdentityService(jdbc, new DataSourceTransactionManager(dataSource));
        assertThat(restarted.resolveVerifiedSubject("subject-one")).isEqualTo(first);
        assertThat(service.resolveVerifiedSubject("subject-two")).isNotEqualTo(first);
        assertCounts(2);
    }

    @Test
    void concurrentFirstLoginsDoNotCreateDuplicatesOrOrphans() throws Exception {
        flyway("2").migrate();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(12)) {
            var futures = new ArrayList<java.util.concurrent.Future<UserPrincipal>>();
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> {
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return service.resolveVerifiedSubject("concurrent-subject");
                }));
            }
            start.countDown();
            var ids = new ArrayList<UUID>();
            for (var future : futures) ids.add(future.get(10, TimeUnit.SECONDS).id());
            assertThat(ids).hasSize(12).containsOnly(ids.getFirst());
        }
        assertCounts(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user u LEFT JOIN external_identity i ON u.id = i.user_id WHERE i.user_id IS NULL",
                Integer.class)).isZero();
    }

    @Test
    void schemaRestrictsUniquenessAndForeignKeysAndContainsOnlyMinimalData() {
        flyway("2").migrate();
        var user = service.resolveVerifiedSubject("schema-subject");
        assertThat(columns("app_user")).containsExactlyInAnyOrder("id", "created_at");
        assertThat(columns("external_identity")).containsExactlyInAnyOrder("user_id", "provider", "issuer", "subject");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO external_identity VALUES (?, 'google', 'https://accounts.google.com', 'schema-subject')", user.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO external_identity VALUES (?, 'google', 'https://accounts.google.com', 'unknown-user')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertCounts(1);
    }

    @Test
    void invalidSubjectsDoNotWriteData() {
        flyway("2").migrate();
        for (String invalid : new String[]{null, "", "   ", "x".repeat(256)}) {
            assertThatThrownBy(() -> service.resolveVerifiedSubject(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        assertCounts(0);
    }

    @Test
    void principalIsSerializableAndContainsOnlyInternalUuid() throws Exception {
        var principal = new UserPrincipal(UUID.randomUUID());
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) { output.writeObject(principal); }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThat(input.readObject()).isEqualTo(principal);
        }
        assertThat(UserPrincipal.class.getRecordComponents()).hasSize(1);
        assertThat(UserPrincipal.class.getRecordComponents()[0].getName()).isEqualTo("id");
    }

    List<String> columns(String table) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                String.class, schema, table);
    }

    void assertCounts(int expected) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM external_identity", Integer.class)).isEqualTo(expected);
    }
}

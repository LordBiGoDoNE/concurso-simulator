package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.application.ResolveExternalIdentityUseCase;
import br.com.concursosimulator.identity.application.port.IdentityAlreadyLinkedException;
import br.com.concursosimulator.identity.application.port.IdentityRepository;
import br.com.concursosimulator.identity.domain.ExternalIdentity;
import br.com.concursosimulator.identity.infrastructure.persistence.JpaIdentityRepository;
import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.shared.infrastructure.SpringUnitOfWork;
import jakarta.persistence.EntityManagerFactory;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class IdentityStorageTests {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    String schema;
    DriverManagerDataSource dataSource;
    JdbcTemplate jdbc;
    EntityManagerFactory factory;
    IdentityRepository repository;
    JpaTransactionManager manager;
    SpringUnitOfWork transactions;

    @BeforeEach
    void database() {
        schema = "identity_" + UUID.randomUUID().toString().replace("-", "");
        String url = POSTGRES.getJdbcUrl();
        dataSource = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema + "&logServerErrorDetail=false",
                POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
    }

    ResolveExternalIdentityUseCase useCase() {
        if (factory == null) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(dataSource);
            bean.setPackagesToScan("br.com.concursosimulator.identity.infrastructure.persistence");
            bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate"));
            bean.afterPropertiesSet();
            factory = bean.getObject();
            manager = new JpaTransactionManager(factory);
            transactions = new SpringUnitOfWork(manager);
            repository = new JpaIdentityRepository(SharedEntityManagerCreator.createSharedEntityManager(factory));
        }
        return new ResolveExternalIdentityUseCase(repository, transactions);
    }

    UUID resolve(String subject) { return useCase().execute(google(subject)); }
    ExternalIdentity google(String subject) { return new ExternalIdentity("google", "https://accounts.google.com", subject); }
    @AfterEach void closeFactory() { if (factory != null) factory.close(); }

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
    void sameSubjectKeepsUuidAcrossPersistenceRestarts() {
        flyway("2").migrate();
        var first = resolve("subject-one");
        factory.close();
        factory = null;
        assertThat(resolve("subject-one")).isEqualTo(first);
        assertThat(resolve("subject-two")).isNotEqualTo(first);
        assertCounts(2);
    }

    @Test
    void concurrentFirstLoginsDoNotCreateDuplicatesOrOrphans(CapturedOutput output) throws Exception {
        flyway("2").migrate();
        var resolver = useCase();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(12)) {
            var futures = new ArrayList<java.util.concurrent.Future<UUID>>();
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> {
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return resolver.execute(google("concurrent-subject"));
                }));
            }
            start.countDown();
            var ids = new ArrayList<UUID>();
            for (var future : futures) ids.add(future.get(10, TimeUnit.SECONDS));
            assertThat(ids).hasSize(12).containsOnly(ids.getFirst());
        }
        assertCounts(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user u LEFT JOIN external_identity i ON u.id = i.user_id WHERE i.user_id IS NULL",
                Integer.class)).isZero();
        assertThat(output.getAll()).doesNotContain("concurrent-subject");
    }

    @Test
    void schemaRestrictsUniquenessAndForeignKeysAndContainsOnlyMinimalData() {
        flyway("2").migrate();
        var user = resolve("schema-subject");
        assertThat(columns("app_user")).containsExactlyInAnyOrder("id", "created_at");
        assertThat(columns("external_identity")).containsExactlyInAnyOrder("user_id", "provider", "issuer", "subject");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO external_identity VALUES (?, 'google', 'https://accounts.google.com', 'schema-subject')", user))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO external_identity VALUES (?, 'google', 'https://accounts.google.com', 'unknown-user')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertCounts(1);
    }

    @Test
    void invalidSubjectsDoNotWriteData() {
        flyway("2").migrate();
        for (String invalid : new String[]{null, "", "   ", "x".repeat(256)}) {
            assertThatThrownBy(() -> resolve(invalid)).isInstanceOf(IllegalArgumentException.class);
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

    @Test
    void opaqueSubjectAndProviderIssuerNamespacesRemainDistinct() {
        flyway("2").migrate();
        var resolver = useCase();
        var first = resolve(" Case-Sensitive ");
        assertThat(resolve("case-sensitive")).isNotEqualTo(first);
        // Provedores sintéticos apenas para testar a porta; não habilitados em produção.
        assertThat(resolver.execute(new ExternalIdentity("fixture", "https://accounts.google.com", " Case-Sensitive "))).isNotEqualTo(first);
        assertThat(resolver.execute(new ExternalIdentity("google", "https://fixture.example", " Case-Sensitive "))).isNotEqualTo(first);
        assertCounts(4);
    }

    @Test
    void duplicateIdentityRollsBackNewUserButUserKeyFailureIsNotIdentityConflict() {
        flyway("2").migrate();
        UUID user = resolve("existing");
        assertThatThrownBy(() -> transactions.independently(() -> {
            repository.createUserWithIdentity(UUID.randomUUID(), google("existing"));
            return null;
        })).isInstanceOf(IdentityAlreadyLinkedException.class);
        assertCounts(1);
        assertThatThrownBy(() -> transactions.independently(() -> {
            repository.createUserWithIdentity(user, google("different"));
            return null;
        })).isNotInstanceOf(IdentityAlreadyLinkedException.class);
        assertCounts(1);
    }

    @Test
    void identityCommitIsIndependentOfAnAmbientTransaction() {
        flyway("2").migrate();
        var resolver = useCase();
        UUID id = new TransactionTemplate(manager).execute(status -> {
            UUID committed = resolver.execute(google("ambient"));
            status.setRollbackOnly();
            return committed;
        });
        assertThat(resolve("ambient")).isEqualTo(id);
        assertCounts(1);
    }

    @Test
    void schemaMismatchFailsValidationInsteadOfBeingAutomaticallyRepaired() {
        flyway("2").migrate();
        jdbc.execute("ALTER TABLE external_identity DROP COLUMN issuer");
        assertThatThrownBy(this::useCase).hasStackTraceContaining("Schema validation");
        assertThat(columns("external_identity")).doesNotContain("issuer");
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

package br.com.concursosimulator.questionbank;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class QuestionSchemaTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    @Test void freshAndUpgradeFromV3PreserveIdentitiesAndSessionsAndValidateJpa() {
        var schema="qb_"+UUID.randomUUID().toString().replace("-", "");
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+"&currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        var jdbc=new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target("3").load().migrate();
        var id=UUID.randomUUID(); jdbc.update("INSERT INTO app_user(id) VALUES (?)",id);
        jdbc.update("INSERT INTO external_identity VALUES (?, 'google', 'https://accounts.google.com', 'fixture')",id);
        jdbc.update("INSERT INTO spring_session VALUES (?, ?, 1, 1, 1800, 1800000, ?)",id.toString(),id.toString(),id.toString());
        jdbc.update("INSERT INTO spring_session_attributes VALUES (?, 'fixture', decode('1234','hex'))",id.toString());
        var users=jdbc.queryForList("SELECT * FROM app_user"); var identities=jdbc.queryForList("SELECT * FROM external_identity");
        var sessions=jdbc.queryForList("SELECT * FROM spring_session");
        var before=jdbc.queryForObject("SELECT encode(attribute_bytes,'hex') FROM spring_session_attributes",String.class);
        assertThat(Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate().migrationsExecuted).isEqualTo(8);
        assertThat(jdbc.queryForList("SELECT * FROM app_user")).isEqualTo(users);
        assertThat(jdbc.queryForList("SELECT * FROM external_identity")).isEqualTo(identities);
        assertThat(jdbc.queryForList("SELECT * FROM spring_session")).isEqualTo(sessions);
        assertThat(jdbc.queryForObject("SELECT encode(attribute_bytes,'hex') FROM spring_session_attributes",String.class)).isEqualTo(before);
        assertCatalogAndJpa(ds,jdbc);
        var fresh="qb_"+UUID.randomUUID().toString().replace("-", "");
        var empty=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+"&currentSchema="+fresh,POSTGRES.getUsername(),POSTGRES.getPassword());
        assertThat(Flyway.configure().dataSource(empty).schemas(fresh).defaultSchema(fresh).load().migrate().migrationsExecuted).isEqualTo(11);
        assertCatalogAndJpa(empty,new JdbcTemplate(empty));
    }
    @Test void constraintsRejectUnknownClassificationOriginDuplicatesAndCrossRevisionAnswers() {
        var schema="qb_"+UUID.randomUUID().toString().replace("-", "");
        var ds=new DriverManagerDataSource(POSTGRES.getJdbcUrl()+"&currentSchema="+schema,POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        var jdbc=new JdbcTemplate(ds); var q=UUID.randomUUID();
        jdbc.update("INSERT INTO qb_question(id,origin_namespace,origin_key) VALUES (?, 'fixture', 'q')",q);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_question(id,origin_namespace,origin_key) VALUES (?, 'fixture', 'q')",UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_topic VALUES ('X01','direito')")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_revision_role VALUES (?, 'professor')",UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
        // FK adiada é conferida no commit de autocommit: resposta sem opção da mesma revisão falha.
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_revision VALUES (?, ?, 1, ?, '{}'::jsonb, 'portugues','P01',?,'DRAFT')",
                UUID.randomUUID(),q,"a".repeat(64),UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM qb_revision",Integer.class)).isZero();
        var revision=UUID.randomUUID(); var option=UUID.randomUUID();
        new TransactionTemplate(new DataSourceTransactionManager(ds)).execute(status -> {
            jdbc.update("INSERT INTO qb_revision VALUES (?, ?, 1, ?, '{}'::jsonb, 'portugues','P01',?,'DRAFT')",revision,q,"a".repeat(64),option);
            jdbc.update("INSERT INTO qb_alternative VALUES (?,?,0),(?,?,1)",revision,option,revision,UUID.randomUUID());
            return null;
        });
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_revision_role VALUES (?, 'professor')",revision)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_alternative VALUES (?,?,2)",revision,option)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_revision VALUES (?, ?, 2, ?, '{}'::jsonb, 'portugues','M01',?,'DRAFT')",UUID.randomUUID(),q,"a".repeat(64),option)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE qb_question SET eligible_revision_id=? WHERE id=?",revision,q)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE qb_revision SET payload='{\"changed\":true}'::jsonb WHERE id=?",revision)).isInstanceOf(DataIntegrityViolationException.class);
        var other=UUID.randomUUID(); jdbc.update("INSERT INTO qb_question(id,origin_namespace,origin_key) VALUES (?, 'fixture','other')",other);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO qb_revision VALUES (?, ?, 1, ?, '{}'::jsonb, 'portugues','P01',?,'DRAFT')",UUID.randomUUID(),other,"a".repeat(64),option)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT eligible_revision_id FROM qb_question WHERE id=?",UUID.class,q)).isNull();
    }
    private void assertCatalogAndJpa(DriverManagerDataSource ds,JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM qb_question",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM qb_topic",Integer.class)).isEqualTo(51);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname=current_schema() AND indexname='me_completed_subject_window_idx'",Integer.class)).isEqualTo(1);
        var bean=new LocalContainerEntityManagerFactoryBean(); bean.setDataSource(ds);
        bean.setPackagesToScan("br.com.concursosimulator.questionbank.infrastructure.persistence");
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));
        bean.afterPropertiesSet(); bean.getObject().close();
    }
}

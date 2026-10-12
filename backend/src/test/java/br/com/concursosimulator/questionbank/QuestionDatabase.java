package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionStore;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.Map;
import java.util.UUID;

/** Banco exclusivo de fixtures, schema aleatório e validate; sem dados/editorial real. */
public final class QuestionDatabase implements AutoCloseable {
    public final JdbcTemplate jdbc;
    public final JpaQuestionStore store;
    public final JpaTransactionManager manager;
    public final EntityManagerFactory factory;
    public QuestionDatabase(PostgreSQLContainer postgres) {
        var schema="qb_"+UUID.randomUUID().toString().replace("-", ""); var url=postgres.getJdbcUrl();
        var ds=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema+"&logServerErrorDetail=false",postgres.getUsername(),postgres.getPassword());
        jdbc=new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).load().migrate();
        var bean=new LocalContainerEntityManagerFactoryBean(); bean.setDataSource(ds);
        bean.setPackagesToScan("br.com.concursosimulator.questionbank.infrastructure.persistence");
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));
        bean.afterPropertiesSet(); factory=bean.getObject(); manager=new JpaTransactionManager(factory);
        store=new JpaQuestionStore(SharedEntityManagerCreator.createSharedEntityManager(factory),manager);
    }
    public void close() { factory.close(); }
}

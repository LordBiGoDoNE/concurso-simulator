package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.QuestionFixtures;
import br.com.concursosimulator.questionbank.infrastructure.persistence.*;
import jakarta.persistence.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
import static br.com.concursosimulator.mockexam.AttemptFixtures.NOW;

public final class AttemptDatabase implements AutoCloseable {
    final DriverManagerDataSource ds;
    final String schema="me_"+UUID.randomUUID().toString().replace("-","");
    final JdbcTemplate jdbc;
    EntityManagerFactory factory;
    EntityManager entities;
    JpaTransactionManager manager;
    JpaQuestionStore questions;
    JpaEligibleRevisionPool pool;
    AttemptDatabase(PostgreSQLContainer pg) { this(pg,"5"); }
    AttemptDatabase(PostgreSQLContainer pg,String target) {
        ds=new DriverManagerDataSource(pg.getJdbcUrl()+"&currentSchema="+schema,pg.getUsername(),pg.getPassword());
        jdbc=new JdbcTemplate(ds); migrate(target);
        if (target.equals("5")) open();
    }
    void migrate(String target) { Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target(target).load().migrate(); }
    void open() {
        var bean=new LocalContainerEntityManagerFactoryBean(); bean.setDataSource(ds);
        bean.setPackagesToScan("br.com.concursosimulator.questionbank.infrastructure.persistence","br.com.concursosimulator.mockexam.infrastructure.persistence");
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate")); bean.afterPropertiesSet();
        factory=bean.getObject(); entities=SharedEntityManagerCreator.createSharedEntityManager(factory);
        manager=new JpaTransactionManager(factory); questions=new JpaQuestionStore(entities,manager); pool=new JpaEligibleRevisionPool(entities);
    }
    Question seed(String text,boolean approve) {
        var q=Question.draft(UUID.randomUUID(),new QuestionOrigin("attempt-fixture",UUID.randomUUID().toString()),UUID.randomUUID(),QuestionFixtures.content(text));
        questions.transaction(s -> { s.insert(q); return null; });
        if (approve) for (var action:List.of(EditorialDecision.Action.SUBMIT,EditorialDecision.Action.APPROVE)) decide(q.id(),action);
        return q;
    }
    void decide(UUID id,EditorialDecision.Action action) {
        questions.transaction(s -> {
            var q=s.loadForUpdate(id).orElseThrow(); var r=q.latest();
            q.decide(new EditorialDecision(action,q.id(),r.id(),r.fingerprint(),"Fixture",NOW,Set.of(EditorialDecision.Check.values()),"Revisão sintética de teste"));
            s.save(q); return null;
        });
    }
    public void close() { if (factory!=null) factory.close(); }
}

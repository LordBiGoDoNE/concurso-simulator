package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.*;
import br.com.concursosimulator.parameterization.infrastructure.persistence.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.postgresql.PostgreSQLContainer;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

/** PostgreSQL/schema descartáveis; nenhuma conexão com o banco editorial do operador. */
final class ParameterDatabase implements AutoCloseable {
    final String schema="pq_"+UUID.randomUUID().toString().replace("-","");
    final DriverManagerDataSource ds;final JdbcTemplate jdbc;
    EntityManagerFactory factory;EntityManager entities;JpaTransactionManager manager;
    JpaQuestionStore questions;JpaTemplateStore store;JpaEligibleTemplates eligible;
    ParameterDatabase(PostgreSQLContainer pg){this(pg,"9");}
    ParameterDatabase(PostgreSQLContainer pg,String target) {
        ds=new DriverManagerDataSource(pg.getJdbcUrl()+"&currentSchema="+schema,pg.getUsername(),pg.getPassword());jdbc=new JdbcTemplate(ds);
        migrate(target);open(!target.equals("5"));
    }
    void migrate(String target){Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target(target).load().migrate();}
    void open(boolean templates) {
        if(factory!=null)factory.close();var bean=new LocalContainerEntityManagerFactoryBean();bean.setDataSource(ds);
        bean.setPackagesToScan(templates?new String[]{"br.com.concursosimulator.questionbank.infrastructure.persistence","br.com.concursosimulator.mockexam.infrastructure.persistence","br.com.concursosimulator.parameterization.infrastructure.persistence"}
                :new String[]{"br.com.concursosimulator.questionbank.infrastructure.persistence","br.com.concursosimulator.mockexam.infrastructure.persistence"});
        bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","validate"));bean.afterPropertiesSet();
        factory=bean.getObject();entities=SharedEntityManagerCreator.createSharedEntityManager(factory);manager=new JpaTransactionManager(factory);questions=new JpaQuestionStore(entities,manager);
        if(templates){store=new JpaTemplateStore(entities,manager);eligible=new JpaEligibleTemplates(entities);}
    }
    TemplateDefinition seed(Family family) {
        var initial=ParameterFixtures.definition(family);var old=br.com.concursosimulator.questionbank.QuestionFixtures.content("Base sintética "+family);
        var content=new QuestionContent(old.statement(),old.alternatives(),old.correctAlternativeId(),old.resolution(),initial.classification(),old.references(),old.provenance());
        var q=Question.draft(initial.baseQuestionId(),new QuestionOrigin("parameter-test",UUID.randomUUID().toString()),initial.baseRevisionId(),content);
        questions.transaction(s -> {s.insert(q);return null;});
        bankDecision(q.id(),EditorialDecision.Action.SUBMIT);bankDecision(q.id(),EditorialDecision.Action.APPROVE);
        return new TemplateDefinition(initial.id(),1,family,NumericGenerator.VERSION,q.id(),q.latest().id(),content.fingerprint(),content.classification(),initial.references(),initial.provenance());
    }
    void bankDecision(UUID qid,EditorialDecision.Action action) {
        questions.transaction(s -> {var q=s.loadForUpdate(qid).orElseThrow();var r=q.latest();
            q.decide(new EditorialDecision(action,q.id(),r.id(),r.fingerprint(),"Fixture",Instant.parse("2026-10-15T00:00:00Z"),Set.of(EditorialDecision.Check.values()),"Teste isolado"));s.save(q);return null;});
    }
    TemplateVersion approve(TemplateDefinition d) {
        var uc=new TemplateUseCases(store);var v=uc.register(d,0).version();
        uc.decide(ParameterFixtures.decision(v,TemplateDecision.Action.SUBMIT),null,0);
        uc.decide(ParameterFixtures.decision(v,TemplateDecision.Action.APPROVE),null,1);return v;
    }
    public void close(){if(factory!=null)factory.close();}
}

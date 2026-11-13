package br.com.concursosimulator.questionbank;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Harnesses que semeiam dados em schemas históricos abrem o JPA atual sobre eles. Antes da V16
 * (#177) não existe qb_decision.review_kind: o atributo é ignorado, como no código daquela época.
 * A escolha decorre da versão real aplicada, nunca de flag do teste; schema atual valida tudo.
 */
public final class LegacySchemaMapping {
    private LegacySchemaMapping() {}
    public static void apply(LocalContainerEntityManagerFactoryBean bean, JdbcTemplate jdbc) {
        var version = jdbc.queryForObject("select max(cast(version as integer)) from flyway_schema_history where success and version is not null", Integer.class);
        if (version != null && version < 16) bean.setMappingResources("legacy/decision-before-v16-orm.xml");
    }
}

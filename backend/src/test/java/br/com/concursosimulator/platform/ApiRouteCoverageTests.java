package br.com.concursosimulator.platform;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Todo mapeamento HTTP de controller tem exatamente uma rota declarada, e toda rota declarada é atendida
 * por um controller ou por um filtro do framework listado aqui.
 *
 * <p>Limite: prova presença e unicidade da política, não que o nível de acesso escolhido esteja certo;
 * isso continua com a revisão e com os testes HTTP de cada rota.
 */
@SpringBootTest
@ActiveProfiles("local")
@Testcontainers
class ApiRouteCoverageTests {
    @Container
    static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:18.6");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
    }

    /** Atendidas por filtros (LogoutFilter, login local administrativo), sem controller. */
    static final Set<String> FRAMEWORK = Set.of("POST /api/v1/auth/logout", "POST /api/v1/admin/auth/login");
    /** Mapeamentos do próprio Spring Boot, fora da API. */
    static final Set<String> FRAMEWORK_MAPPINGS = Set.of("/error");

    @Autowired
    ApiRoutes routes;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void everyControllerMappingHasExactlyOneDeclaredRouteAndEveryRouteIsServed() {
        var declared = new HashMap<String, Integer>();
        for (var area : routes.areas())
            for (var route : area.routes())
                declared.merge(route.method() + " " + ApiRoutes.normalized(route.pattern(), "{}"), 1, Integer::sum);
        var mapped = new TreeSet<String>();
        var missing = new ArrayList<String>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            for (var pattern : info.getPatternValues()) {
                if (FRAMEWORK_MAPPINGS.contains(pattern)) continue;
                var methods = info.getMethodsCondition().getMethods();
                assertThat(methods)
                        .as("mapeamento sem método explícito: %s %s", handler, pattern)
                        .isNotEmpty();
                for (var method : methods) {
                    var key = method.name() + " " + ApiRoutes.normalized(pattern, "{}");
                    mapped.add(key);
                    if (declared.getOrDefault(key, 0) != 1) missing.add(key + " (" + handler + ")");
                }
            }
        });
        assertThat(missing).as("mapeamentos sem exatamente uma rota declarada").isEmpty();
        var orphan = new TreeSet<>(declared.keySet());
        orphan.removeAll(mapped);
        orphan.removeAll(FRAMEWORK);
        assertThat(orphan)
                .as("rotas declaradas sem controller nem filtro do framework")
                .isEmpty();
        assertThat(declared.keySet())
                .as("rotas do framework precisam continuar declaradas")
                .containsAll(FRAMEWORK);
    }

    @Test
    void frameworkListOnlyNamesRoutesWithoutController() {
        var mapped = new TreeSet<String>();
        mappings.getHandlerMethods()
                .forEach((info, handler) -> info.getPatternValues()
                        .forEach(pattern -> info.getMethodsCondition()
                                .getMethods()
                                .forEach(m -> mapped.add(m.name() + " " + pattern))));
        assertThat(mapped).doesNotContainAnyElementsOf(FRAMEWORK);
        assertThat(List.copyOf(mapped)).isNotEmpty();
    }
}

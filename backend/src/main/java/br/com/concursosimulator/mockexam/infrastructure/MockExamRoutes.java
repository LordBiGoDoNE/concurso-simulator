package br.com.concursosimulator.mockexam.infrastructure;

import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Rotas de simulados e práticas de aula; posse das tentativas continua na aplicação/domínio. */
@Configuration
public class MockExamRoutes {
    public static final String AREA = "mockexam.attempts";
    static final String ATTEMPTS = "/api/v1/attempts";
    static final String PRACTICES = "/api/v1/lesson-practices";

    @Bean
    ApiArea mockExamArea() {
        var paths = List.of(
                ATTEMPTS,
                ATTEMPTS + "/**",
                "/api/v1/me/attempts",
                PRACTICES,
                PRACTICES + "/**",
                "/api/v1/me/lesson-practices");
        var routes = new ArrayList<ApiRoute>();
        for (var root : List.of(ATTEMPTS, PRACTICES)) {
            routes.add(ApiRoute.post(root, VISITOR_OR_ACCOUNT));
            routes.add(ApiRoute.get(root + "/{id}", VISITOR_OR_ACCOUNT));
            routes.add(ApiRoute.get(root + "/{id}/result", VISITOR_OR_ACCOUNT));
            routes.add(ApiRoute.post(root + "/{id}/finish", VISITOR_OR_ACCOUNT));
            routes.add(ApiRoute.put(root + "/{id}/answers/{item}", VISITOR_OR_ACCOUNT));
        }
        routes.add(ApiRoute.get(PRACTICES + "/availability", VISITOR_OR_ACCOUNT));
        routes.add(ApiRoute.get("/api/v1/me/attempts", ACCOUNT));
        routes.add(ApiRoute.get("/api/v1/me/lesson-practices", ACCOUNT));
        return new ApiArea(
                AREA, ApiArea.Chain.SESSION, paths, Cors.credentialed("GET", "POST", "PUT"), paths, true, routes);
    }
}

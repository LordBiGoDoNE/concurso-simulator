package br.com.concursosimulator.subjectreview.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;

/**
 * Rotas da revisão por assunto. A revisão individual fica sob {@code /api/v1/attempts/**}, cujo CORS
 * já é declarado pelo simulado; aqui só a visão geral registra CORS.
 */
@Configuration
public class SubjectReviewRoutes {
    public static final String AREA = "subjectreview.review";
    static final String OVERVIEW = "/api/v1/me/subject-review";

    @Bean
    ApiArea subjectReviewArea() {
        return new ApiArea(AREA, ApiArea.Chain.SESSION, List.of(OVERVIEW, "/api/v1/attempts/*/subject-review"),
                Cors.credentialed("GET", "POST"), List.of(OVERVIEW), true,
                List.of(ApiRoute.get(OVERVIEW, ACCOUNT), ApiRoute.get("/api/v1/attempts/{id}/subject-review", VISITOR_OR_ACCOUNT)));
    }
}

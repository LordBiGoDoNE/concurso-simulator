package br.com.concursosimulator.questionbank.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;

/** Rotas públicas, de importação e de curadoria do banco de questões. */
@Configuration
public class QuestionBankRoutes {
    public static final String PUBLIC_AREA = "questionbank.public";
    public static final String IMPORT_AREA = "questionbank.import";
    public static final String CURATION_AREA = "questionbank.curation";
    static final String PUBLIC_ROOT = "/api/v1/question-bank";
    static final String IMPORTS = "/api/v1/admin/question-imports";
    static final String QUESTIONS = "/api/v1/admin/questions";
    public static final String EDITORIAL_DECISIONS = "/api/v1/admin/editorial-decisions";
    private static final String UUID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Bean
    ApiArea questionBankPublicArea() {
        return new ApiArea(PUBLIC_AREA, ApiArea.Chain.STATELESS, List.of(PUBLIC_ROOT, PUBLIC_ROOT + "/**"), Cors.publicRead(),
                List.of(PUBLIC_ROOT + "/catalog", PUBLIC_ROOT + "/questions", PUBLIC_ROOT + "/questions/*"), false,
                List.of(ApiRoute.get(PUBLIC_ROOT + "/catalog", PUBLIC),
                        ApiRoute.get(PUBLIC_ROOT + "/questions", PUBLIC),
                        ApiRoute.get(PUBLIC_ROOT + "/questions/{questionId}", PUBLIC)));
    }

    @Bean
    ApiArea questionImportArea() {
        var paths = List.of(IMPORTS, IMPORTS + "/**");
        return new ApiArea(IMPORT_AREA, ApiArea.Chain.OWN, paths, Cors.credentialed("POST"), paths, false,
                List.of(ApiRoute.post(IMPORTS + "/preview", ADMINISTRATIVE), ApiRoute.post(IMPORTS + "/apply", ADMINISTRATIVE)));
    }

    @Bean
    ApiArea questionCurationArea() {
        var paths = List.of(QUESTIONS, QUESTIONS + "/**", EDITORIAL_DECISIONS, EDITORIAL_DECISIONS + "/**");
        return new ApiArea(CURATION_AREA, ApiArea.Chain.OWN, paths, Cors.credentialed("GET", "POST"), paths, false,
                List.of(ApiRoute.get(QUESTIONS, ADMINISTRATIVE),
                        ApiRoute.get(QUESTIONS + "/{questionId:" + UUID + "}", ADMINISTRATIVE),
                        ApiRoute.post(QUESTIONS + "/lookup", ADMINISTRATIVE),
                        ApiRoute.post(QUESTIONS + "/export", ADMINISTRATIVE),
                        ApiRoute.post(EDITORIAL_DECISIONS + "/preview", ADMINISTRATIVE),
                        ApiRoute.post(EDITORIAL_DECISIONS + "/apply", ADMINISTRATIVE)));
    }
}

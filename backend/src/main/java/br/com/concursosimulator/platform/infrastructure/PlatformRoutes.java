package br.com.concursosimulator.platform.infrastructure;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Prontidão pública, sem sessão. */
@Configuration
public class PlatformRoutes {
    static final String STATUS = "/api/v1/status";

    @Bean
    ApiArea platformStatusArea() {
        return new ApiArea(
                "platform.status",
                ApiArea.Chain.STATELESS,
                List.of(STATUS),
                Cors.publicRead(),
                List.of(STATUS),
                false,
                List.of(ApiRoute.get(STATUS, ApiRoute.Access.PUBLIC)));
    }
}

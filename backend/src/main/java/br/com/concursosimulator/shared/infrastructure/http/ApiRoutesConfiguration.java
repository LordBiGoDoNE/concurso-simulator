package br.com.concursosimulator.shared.infrastructure.http;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Agrega as áreas HTTP declaradas pelas funcionalidades num registro único. */
@Configuration
public class ApiRoutesConfiguration {
    @Bean
    ApiRoutes apiRoutes(List<ApiArea> areas) { return new ApiRoutes(areas); }
}

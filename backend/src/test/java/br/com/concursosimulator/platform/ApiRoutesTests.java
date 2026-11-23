package br.com.concursosimulator.platform;

import br.com.concursosimulator.shared.infrastructure.http.ApiArea;
import br.com.concursosimulator.shared.infrastructure.http.ApiArea.Cors;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static br.com.concursosimulator.shared.infrastructure.http.ApiRoute.Access.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras do registro de rotas, sem contexto Spring. */
class ApiRoutesTests {
    static ApiArea area(String name, List<String> paths, ApiRoute... routes) {
        return new ApiArea(name, ApiArea.Chain.SESSION, paths, null, List.of(), false, List.of(routes));
    }

    static MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void duplicateRouteFailsAtStartupNamingTheRoute() {
        assertThatThrownBy(() -> new ApiRoutes(List.of(
                area("a", List.of("/api/v1/x/**"), ApiRoute.get("/api/v1/x/{id}", PUBLIC)),
                area("b", List.of("/api/v1/x/**"), ApiRoute.get("/api/v1/x/{id}", ACCOUNT)))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("GET /api/v1/x/{id}");
    }

    @Test
    void duplicateAreaNameFails() {
        assertThatThrownBy(() -> new ApiRoutes(List.of(area("a", List.of("/x")), area("a", List.of("/y")))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("a");
    }

    @Test
    void routeOutsideItsAreaFails() {
        assertThatThrownBy(() -> new ApiRoutes(List.of(area("a", List.of("/api/v1/a/**"), ApiRoute.get("/api/v1/b", PUBLIC)))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("GET /api/v1/b");
    }

    @Test
    void mostSpecificRouteAndAreaWin() {
        var routes = new ApiRoutes(List.of(
                area("general", List.of("/api/v1/items/**"), ApiRoute.get("/api/v1/items/{id}", VISITOR_OR_ACCOUNT)),
                area("specific", List.of("/api/v1/items/*/review"), ApiRoute.get("/api/v1/items/{id}/review", ACCOUNT)),
                area("named", List.of("/api/v1/named/**"), ApiRoute.get("/api/v1/named/{id}", PUBLIC), ApiRoute.get("/api/v1/named/special", ACCOUNT))));
        assertThat(routes.area(request("GET", "/api/v1/items/1/review"))).map(ApiArea::name).contains("specific");
        assertThat(routes.area(request("GET", "/api/v1/items/1"))).map(ApiArea::name).contains("general");
        assertThat(routes.route(request("GET", "/api/v1/named/special"))).map(ApiRoute::access).contains(ACCOUNT);
        assertThat(routes.route(request("GET", "/api/v1/named/other"))).map(ApiRoute::access).contains(PUBLIC);
    }

    @Test
    void matchingUsesMethodServletPathAndNoOptionalTrailingSlash() {
        var routes = new ApiRoutes(List.of(area("a", List.of("/api/v1/a", "/api/v1/a/**"),
                ApiRoute.get("/api/v1/a", PUBLIC), ApiRoute.get("/api/v1/a/{id}", PUBLIC))));
        assertThat(routes.permits(PUBLIC, request("GET", "/api/v1/a"))).isTrue();
        assertThat(routes.permits(PUBLIC, request("POST", "/api/v1/a"))).isFalse();
        assertThat(routes.permits(PUBLIC, request("GET", "/api/v1/a/"))).isFalse();
        assertThat(routes.permits(PUBLIC, request("GET", "/api/v1/a/x/y"))).isFalse();
        assertThat(routes.owns("a", request("DELETE", "/api/v1/a/x/y"))).isTrue();
    }

    @Test
    void regexVariablesAndPasswordExemptionByPath() {
        var routes = new ApiRoutes(List.of(area("a", List.of("/api/v1/q", "/api/v1/q/**", "/api/v1/me"),
                ApiRoute.get("/api/v1/q/{id:[0-9a-f]{4}}", ADMINISTRATIVE),
                ApiRoute.get("/api/v1/me", ACCOUNT_PRINCIPAL).exemptFromPasswordChange())));
        assertThat(routes.permits(ADMINISTRATIVE, request("GET", "/api/v1/q/abcd"))).isTrue();
        assertThat(routes.permits(ADMINISTRATIVE, request("GET", "/api/v1/q/abcde"))).isFalse();
        assertThat(routes.passwordChangeExempt(request("DELETE", "/api/v1/me"))).isTrue();
        assertThat(routes.passwordChangeExempt(request("GET", "/api/v1/q/abcd"))).isFalse();
        assertThat(ApiRoutes.normalized("/api/v1/q/{id:[0-9a-f]{4}}/x/{y}", "{}")).isEqualTo("/api/v1/q/{}/x/{}");
    }

    @Test
    void corsRejectsWildcardOriginsAndIncompleteDeclarations() {
        var routes = new ApiRoutes(List.of(new ApiArea("a", ApiArea.Chain.SESSION, List.of("/a"), Cors.publicRead(), List.of("/a"), false, List.of())));
        assertThatThrownBy(() -> routes.cors(List.of("https://*.example"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApiArea("b", ApiArea.Chain.SESSION, List.of("/b"), Cors.publicRead(), List.of(), false, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

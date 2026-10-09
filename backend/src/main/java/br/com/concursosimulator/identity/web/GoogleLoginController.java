package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.port.LoginOptions;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleLoginController {
    private final LoginOptions properties;
    public GoogleLoginController(LoginOptions properties) { this.properties = properties; }

    @GetMapping("/api/v1/auth/config")
    ResponseEntity<?> config() {
        return ResponseEntity.ok().body(Map.of("googleEnabled", properties.enabled()));
    }

    // Se habilitado, os filtros OAuth interceptam essas rotas antes do controller.
    @GetMapping({"/oauth2/authorization/google", "/login/oauth2/code/google"})
    ResponseEntity<Void> unavailable() { return ResponseEntity.notFound().build(); }
}

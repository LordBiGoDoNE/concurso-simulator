package br.com.concursosimulator.identity;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    @GetMapping("/api/v1/me")
    ResponseEntity<?> me(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("id", principal.id()));
        }
        return ResponseEntity.status(401).cacheControl(CacheControl.noStore())
                .body(Map.of("error", "unauthenticated"));
    }

    @GetMapping("/api/v1/csrf")
    ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }
}

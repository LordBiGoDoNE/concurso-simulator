package br.com.concursosimulator.identity.web;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    @GetMapping("/api/v1/me")
    ResponseEntity<MeResponse> me(@AuthenticationPrincipal(errorOnInvalidType = true) UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new MeResponse(principal.id()));
    }

    @GetMapping("/api/v1/csrf")
    ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getToken(), token.getHeaderName()));
    }

    public record MeResponse(UUID id) {}
    public record CsrfResponse(String token, String headerName) {}
}

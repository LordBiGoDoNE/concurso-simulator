package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import br.com.concursosimulator.identity.application.port.AdministrativeCapabilities;
import br.com.concursosimulator.identity.application.port.LoginOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/account")
public class AdministrativeAccountController {
    private final AdministrativeAccessUseCases access;
    private final AdministrativeSession sessions;
    private final AdministrativeCapabilities capabilities;
    private final LoginOptions google;
    public AdministrativeAccountController(AdministrativeAccessUseCases access,AdministrativeSession sessions,AdministrativeCapabilities capabilities,LoginOptions google) {
        this.access=access;this.sessions=sessions;this.capabilities=capabilities;this.google=google;
    }
    public record AccountResponse(String username,boolean mustChangePassword,boolean googleEnabled,boolean googleLinked,boolean questionImportAvailable) {}
    public record PasswordRequest(String currentPassword,String newPassword) {
        @Override public String toString(){return "PasswordRequest[redacted]";}
    }
    public record LinkRequest(String currentPassword) { @Override public String toString(){return "LinkRequest[redacted]";} }
    public record LinkResponse(String authorizationUrl) {}
    @GetMapping
    public ResponseEntity<AccountResponse> account(@AuthenticationPrincipal UserPrincipal actor,@RequestAttribute("admin.epoch")long epoch) {
        var a=access.current(actor.id(),epoch,true);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new AccountResponse(a.username(),a.mustChange(),google.enabled(),access.linked(actor.id()),!a.mustChange() && capabilities.questionImportAvailable()));
    }
    @PostMapping("/password")
    public ResponseEntity<Void> password(@AuthenticationPrincipal UserPrincipal actor,@RequestAttribute("admin.epoch")long epoch,@RequestBody PasswordRequest input) {
        var updated=access.changePassword(actor.id(),epoch,input.currentPassword(),input.newPassword());sessions.replace(updated);
        return ResponseEntity.noContent().header("Cache-Control","no-store").build();
    }
    @PostMapping("/google-link")
    public ResponseEntity<LinkResponse> link(@AuthenticationPrincipal UserPrincipal actor,@RequestAttribute("admin.epoch")long epoch,@RequestBody LinkRequest input) {
        var a=access.verifyPassword(actor.id(),epoch,input.currentPassword(),false);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new LinkResponse(sessions.beginGoogleLink(a)));
    }
}

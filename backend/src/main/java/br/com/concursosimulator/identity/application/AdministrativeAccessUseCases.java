package br.com.concursosimulator.identity.application;

import br.com.concursosimulator.identity.application.port.*;
import br.com.concursosimulator.identity.domain.*;
import java.time.Clock;
import java.util.UUID;

/** Atomic administrative actions. Technical session and OIDC proof belong to the adapters. */
public final class AdministrativeAccessUseCases {
    private final AdministrativeAccounts accounts;
    private final AdministrativePasswords passwords;
    private final Clock clock;
    private final String dummy;
    public AdministrativeAccessUseCases(AdministrativeAccounts accounts, AdministrativePasswords passwords, Clock clock) {
        this.accounts=accounts; this.passwords=passwords; this.clock=clock;
        dummy=passwords.encode(UUID.randomUUID().toString());
    }
    public record Access(UUID owner, long version, String username, boolean mustChange) {}
    public static final class Failure extends RuntimeException {
        private final String code;
        public Failure(String code) { super(code); this.code=code; }
        public String code() { return code; }
    }
    private static Access access(AdministrativeAccount a) { return new Access(a.owner(),a.version(),a.username(),a.mustChange()); }
    public boolean bootstrap(String username, String initial, boolean temporary) {
        return accounts.atomic(s -> {
            if(s.current().isPresent()) return false;
            AdministrativeAccount.validatePassword(initial);
            var id=UUID.randomUUID();
            var a=new AdministrativeAccount(id,username,passwords.encode(initial),temporary,true,1,0,null,null);
            s.createUser(id); s.save(a); return true;
        });
    }
    public Access login(String username, String password) {
        var result=accounts.atomic(s -> {
            var found=s.current();
            if(found.isEmpty()) { matches(password,dummy); return new Outcome(null,"INVALID_CREDENTIALS"); }
            var a=found.get();
            boolean match=matches(password,a.passwordHash());
            if(a.blocked(clock.instant())) return new Outcome(null,"LOGIN_THROTTLED");
            if(!a.enabled() || !a.username().equals(username) || !match) {
                s.save(a.failed(clock.instant())); return new Outcome(null,"INVALID_CREDENTIALS");
            }
            s.save(a.authenticated()); return new Outcome(access(a),null);
        });
        if(result.failure!=null) throw new Failure(result.failure); return result.access;
    }
    private record Outcome(Access access,String failure) {}
    private boolean matches(String password,String hash) {
        boolean valid=true;
        try{AdministrativeAccount.validatePassword(password);}catch(IllegalArgumentException invalid){valid=false;}
        boolean result=passwords.matches(valid?password:"invalid-input",valid?hash:dummy);
        return valid && result;
    }
    public Access current(UUID actor,long epoch,boolean allowTemporary) {
        var a=accounts.current().filter(v->v.permits(actor,epoch,allowTemporary)).orElseThrow(()->new Failure("ADMIN_ACCESS_DENIED"));
        return access(a);
    }
    public Access googleAccess(UUID actor) {
        return accounts.current().filter(a->a.enabled() && a.owner().equals(actor)).map(AdministrativeAccessUseCases::access).orElse(null);
    }
    public boolean active(UUID actor) { return accounts.active(actor); }
    public Access changePassword(UUID actor,long epoch,String oldPassword,String newPassword) {
        AdministrativeAccount.validatePassword(newPassword);
        // Failed proof is committed to the same attempt counter used by login.
        verifyPassword(actor,epoch,oldPassword,true);
        return accounts.atomic(s -> {
            var a=require(s,actor,epoch,true);
            if(passwords.matches(newPassword,a.passwordHash())) throw new Failure("PASSWORD_UNCHANGED");
            var changed=a.password(passwords.encode(newPassword),false); s.save(changed); return access(changed);
        });
    }
    public Access verifyPassword(UUID actor,long epoch,String password,boolean allowTemporary) {
        var outcome=accounts.atomic(s -> {
            var a=require(s,actor,epoch,allowTemporary);
            boolean match=matches(password,a.passwordHash());
            if(a.blocked(clock.instant())) return new Outcome(null,"LOGIN_THROTTLED");
            if(!match) { s.save(a.failed(clock.instant())); return new Outcome(null,"INVALID_CREDENTIALS"); }
            s.save(a.authenticated()); return new Outcome(access(a),null);
        });
        if(outcome.failure!=null) throw new Failure(outcome.failure); return outcome.access;
    }
    public void recover(String temporary) {
        AdministrativeAccount.validatePassword(temporary);
        accounts.atomic(s -> { var a=s.current().orElseThrow(()->new Failure("ADMIN_NOT_INITIALIZED"));
            s.save(a.password(passwords.encode(temporary),true)); return null; });
    }
    public Access link(UUID actor,long epoch,ExternalIdentity external) {
        if(!external.provider().equals("google") || !external.issuer().equals("https://accounts.google.com")) throw new Failure("INVALID_LINK");
        for(int retry=0;;retry++) {
            try { return accounts.atomic(s -> {
                var a=require(s,actor,epoch,false);
                var existing=s.owner(external);
                UUID target=existing.orElse(actor);
                s.lockUsers(actor,target);
                if(s.hasGoogle(actor) && (existing.isEmpty() || !target.equals(actor))) throw new Failure("GOOGLE_ALREADY_LINKED");
                if(!target.equals(actor)) {
                    if(s.hasHistory(actor)) throw new Failure("LOCAL_ACCOUNT_HAS_HISTORY");
                    s.deactivate(actor);
                }
                if(existing.isEmpty()) s.attach(external,actor);
                var changed=a.linked(target); s.save(changed); return access(changed);
            }); } catch(IdentityAlreadyLinkedException conflict) { if(retry>=2) throw conflict; }
        }
    }
    public boolean linked(UUID actor) { return accounts.atomic(s -> s.hasGoogle(actor)); }
    private static AdministrativeAccount require(AdministrativeAccounts.Session s,UUID actor,long epoch,boolean temporary) {
        return s.current().filter(a->a.permits(actor,epoch,temporary)).orElseThrow(()->new Failure("ADMIN_ACCESS_DENIED"));
    }
}

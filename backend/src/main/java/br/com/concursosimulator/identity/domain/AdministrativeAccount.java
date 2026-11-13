package br.com.concursosimulator.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Credential owner and authentication epoch; no recoverable password or provider tokens. */
public record AdministrativeAccount(UUID owner, String username, String passwordHash, boolean mustChange,
        boolean enabled, long version, int failures, Instant failureWindow, Instant blockedUntil) {
    public AdministrativeAccount {
        Objects.requireNonNull(owner); Objects.requireNonNull(passwordHash);
        if (username == null || !username.matches("[A-Za-z0-9._-]{1,64}") || version < 0 || failures < 0)
            throw new IllegalArgumentException("Invalid administrative account");
    }
    @Override public String toString(){return "AdministrativeAccount[credential redacted]";}
    public boolean blocked(Instant now) { return blockedUntil != null && now.isBefore(blockedUntil); }
    public AdministrativeAccount failed(Instant now) {
        int count = failureWindow == null || !now.isBefore(failureWindow.plusSeconds(900)) ? 1 : failures + 1;
        return new AdministrativeAccount(owner, username, passwordHash, mustChange, enabled, version, count,
                count == 1 ? now : failureWindow, count >= 5 ? now.plusSeconds(900) : null);
    }
    public AdministrativeAccount authenticated() {
        return new AdministrativeAccount(owner, username, passwordHash, mustChange, enabled, version, 0, null, null);
    }
    public AdministrativeAccount password(String hash, boolean temporary) {
        return new AdministrativeAccount(owner, username, hash, temporary, enabled, version + 1, 0, null, null);
    }
    public AdministrativeAccount linked(UUID target) {
        return new AdministrativeAccount(target, username, passwordHash, mustChange, enabled, version + 1, 0, null, null);
    }
    public boolean permits(UUID actor, long epoch, boolean allowTemporary) {
        return enabled && owner.equals(actor) && version == epoch && (allowTemporary || !mustChange);
    }
    public static void validatePassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 15 || password.codePointCount(0,password.length()) > 128)
            throw new IllegalArgumentException("Password must contain 15 to 128 characters");
        for(int i=0;i<password.length();i++) {
            char c=password.charAt(i);
            if(Character.isHighSurrogate(c)) {
                if(i+1>=password.length() || !Character.isLowSurrogate(password.charAt(++i)))throw new IllegalArgumentException("Invalid password encoding");
            } else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("Invalid password encoding");
        }
    }
}

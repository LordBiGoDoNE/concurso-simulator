package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.identity.web.UserPrincipal;
import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

/** Identidade técnica verificada antes do controller; não decide posse de recursos. */
@Component
public final class AttemptAccess {
    public static final String ACTOR=AttemptOwner.class.getName();
    public static final String VISITOR="mockexam.visitor";
    private final AuthenticationTrustResolverImpl trust=new AuthenticationTrustResolverImpl();
    public AuthorizationDecision authorize(Authentication authentication,HttpServletRequest request,boolean accountOnly) {
        AttemptOwner actor;
        if (trust.isAuthenticated(authentication) && authentication.getPrincipal() instanceof UserPrincipal p) {
            actor=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,p.id());
        } else if (!accountOnly && (authentication==null || trust.isAnonymous(authentication)
                && "anonymousUser".equals(authentication.getPrincipal()))) {
            var session=request.getSession();
            var id=session.getAttribute(VISITOR);
            if (id==null) { id=visitorId(session.getId()); session.setAttribute(VISITOR,id); }
            if (!(id instanceof UUID uuid)) return new AuthorizationDecision(false);
            actor=new AttemptOwner(AttemptOwner.Kind.VISITOR,uuid);
        } else return new AuthorizationDecision(false);
        request.setAttribute(ACTOR,actor); return new AuthorizationDecision(true);
    }
    private static UUID visitorId(String randomSessionId) {
        // Duas requisições iniciais da mesma sessão devem produzir o mesmo UUID,
        // inclusive em processos distintos. O identificador da sessão nunca sai desta infraestrutura.
        try {
            var hash=java.security.MessageDigest.getInstance("SHA-256").digest(
                    (VISITOR+":"+randomSessionId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var bytes=java.nio.ByteBuffer.wrap(hash); return new UUID(bytes.getLong(),bytes.getLong());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}

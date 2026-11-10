package br.com.concursosimulator.identity.web;

import br.com.concursosimulator.identity.domain.ExternalIdentity;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/** OIDC adapter hook: input is already verified by Spring Security. */
public interface AdministrativeGoogleCompletion {
    boolean completeLink(HttpServletRequest request,HttpServletResponse response,ExternalIdentity identity) throws IOException;
    Authentication googleAuthentication(UUID user);
}

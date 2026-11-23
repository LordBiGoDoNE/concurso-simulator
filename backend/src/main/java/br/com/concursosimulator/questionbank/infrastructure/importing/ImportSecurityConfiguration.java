package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.questionbank.infrastructure.QuestionBankRoutes;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoute;
import br.com.concursosimulator.shared.infrastructure.http.ApiRoutes;
import br.com.concursosimulator.shared.infrastructure.http.JsonRejection;
import br.com.concursosimulator.identity.application.UserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class ImportSecurityConfiguration {
    @Bean @Order(0)
    SecurityFilterChain administrativeImportSecurity(HttpSecurity http,ImportSettings settings,
            br.com.concursosimulator.questionbank.application.port.AdministrativeImportPermission permission,
            HttpSessionSecurityContextRepository repository,ApiRoutes routes)throws Exception {
        return http.securityMatcher(routes.paths(QuestionBankRoutes.IMPORT_AREA))
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .securityContext(c->c.securityContextRepository(repository))
                .requestCache(c->c.requestCache(new NullRequestCache()))
                .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a->a.requestMatchers(r->routes.permits(ApiRoute.Access.ADMINISTRATIVE,r))
                        .access((authentication,context)->{
                            var current=authentication.get();return new AuthorizationDecision(valid(current) && current.getDetails() instanceof br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence evidence && permission.permits(((UserPrincipal)current.getPrincipal()).id(),evidence.version()));
                        }).anyRequest().denyAll())
                .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->reject(s,401,"UNAUTHENTICATED"))
                        .accessDeniedHandler((r,s,x)->{
                            int status=x instanceof org.springframework.security.web.csrf.CsrfException?403:valid(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())?403:401;
                            reject(s,status,status==401?"UNAUTHENTICATED":"FORBIDDEN");
                        }))
                .addFilterBefore(new NoStore(),org.springframework.web.filter.CorsFilter.class)
                .addFilterAfter(new Capacity(settings),AuthorizationFilter.class).build();
    }
    static boolean valid(Authentication auth){return new AuthenticationTrustResolverImpl().isAuthenticated(auth) && auth.getPrincipal() instanceof UserPrincipal;}
    static void reject(HttpServletResponse response,int status,String code)throws IOException {
        JsonRejection.write(response,status,code);
    }
    static final class NoStore extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException {
            s.setHeader("Cache-Control","no-store");chain.doFilter(r,s);
        }
    }
    static final class Capacity extends OncePerRequestFilter {
        private final Semaphore capacity=new Semaphore(1);
        private final ImportSettings settings;
        Capacity(){this.settings=null;} // Unit boundary tests exercise capacity independently.
        Capacity(ImportSettings settings){this.settings=settings;}
        @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException {
            if(settings!=null && !settings.configured()){JsonRejection.writeCorrelated(s,503,"IMPORT_NOT_CONFIGURED");return;}
            try {
                var type=org.springframework.http.MediaType.parseMediaType(r.getContentType());
                if(!"application".equals(type.getType()) || !"json".equals(type.getSubtype()) || type.getCharset()!=null && !java.nio.charset.StandardCharsets.UTF_8.equals(type.getCharset())){reject(s,415,"UNSUPPORTED_MEDIA_TYPE");return;}
            }catch(IllegalArgumentException invalid){reject(s,415,"UNSUPPORTED_MEDIA_TYPE");return;}
            if(!"identity".equalsIgnoreCase(Optional.ofNullable(r.getHeader("Content-Encoding")).orElse("identity"))){reject(s,415,"UNSUPPORTED_CONTENT_ENCODING");return;}
            if(r.getContentLengthLong()>16_777_216){reject(s,413,"REQUEST_TOO_LARGE");return;}
            if(!capacity.tryAcquire()){s.setHeader("Retry-After","1");reject(s,429,"IMPORT_BUSY");return;}
            try{chain.doFilter(r,s);}finally{capacity.release();}
        }
    }
}

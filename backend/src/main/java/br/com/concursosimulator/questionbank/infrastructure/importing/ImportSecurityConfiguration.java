package br.com.concursosimulator.questionbank.infrastructure.importing;

import br.com.concursosimulator.identity.web.UserPrincipal;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class ImportSecurityConfiguration {
    @Bean @Order(0)
    SecurityFilterChain administrativeImportSecurity(HttpSecurity http,ImportSettings settings,
            br.com.concursosimulator.questionbank.application.port.AdministrativeImportPermission permission,
            HttpSessionSecurityContextRepository repository,@Value("${app.cors-origins}")String origins)throws Exception {
        var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());
        if(cors.getAllowedOrigins().stream().anyMatch(o->o.contains("*")))throw new IllegalArgumentException("Origem explícita obrigatória");
        cors.setAllowCredentials(true);cors.setAllowedMethods(List.of("POST"));cors.setAllowedHeaders(List.of("Content-Type","Accept","X-CSRF-TOKEN"));
        return http.securityMatcher("/api/v1/admin/question-imports","/api/v1/admin/question-imports/**")
                .cors(c->c.configurationSource(r->cors))
                .securityContext(c->c.securityContextRepository(repository))
                .requestCache(c->c.requestCache(new NullRequestCache()))
                .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(a->a.requestMatchers(r->r.getMethod().equals("POST") && Set.of("/api/v1/admin/question-imports/preview","/api/v1/admin/question-imports/apply").contains(r.getServletPath()))
                        .access((authentication,context)->{
                            var current=authentication.get();return new AuthorizationDecision(valid(current) && current.getDetails() instanceof br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity.Evidence evidence && permission.permits(((UserPrincipal)current.getPrincipal()).id(),evidence.version()));
                        }).anyRequest().denyAll())
                .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->reject(s,401,"unauthenticated"))
                        .accessDeniedHandler((r,s,x)->{
                            int status=x instanceof org.springframework.security.web.csrf.CsrfException?403:valid(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())?403:401;
                            reject(s,status,status==401?"unauthenticated":"forbidden");
                        }))
                .addFilterBefore(new NoStore(),org.springframework.web.filter.CorsFilter.class)
                .addFilterAfter(new Capacity(settings),AuthorizationFilter.class).build();
    }
    static boolean valid(Authentication auth){return new AuthenticationTrustResolverImpl().isAuthenticated(auth) && auth.getPrincipal() instanceof UserPrincipal;}
    static void reject(HttpServletResponse response,int status,String code)throws IOException {
        response.setStatus(status);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
        response.getWriter().write("{\"error\":\""+code+"\"}");
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
            if(settings!=null && !settings.configured()){s.setStatus(503);s.setContentType("application/json");s.getWriter().write("{\"error\":\"IMPORT_NOT_CONFIGURED\",\"correlationId\":\""+UUID.randomUUID()+"\"}");return;}
            if(!"identity".equalsIgnoreCase(Optional.ofNullable(r.getHeader("Content-Encoding")).orElse("identity"))){reject(s,415,"UNSUPPORTED_CONTENT_ENCODING");return;}
            if(r.getContentLengthLong()>16_777_216){reject(s,413,"REQUEST_TOO_LARGE");return;}
            if(!capacity.tryAcquire()){s.setHeader("Retry-After","1");reject(s,429,"IMPORT_BUSY");return;}
            try{chain.doFilter(r,s);}finally{capacity.release();}
        }
    }
}

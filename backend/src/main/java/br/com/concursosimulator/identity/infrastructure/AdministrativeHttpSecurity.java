package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AdministrativeHttpSecurity {
    @Bean @Order(-1)
    SecurityFilterChain administrativeAccountSecurity(HttpSecurity http,AdministrativeSecurity security,
            AdministrativeAccessUseCases access,HttpSessionSecurityContextRepository repository) throws Exception {
        return http.securityMatcher("/api/v1/admin/auth/**","/api/v1/admin/account","/api/v1/admin/account/**")
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .securityContext(c->c.securityContextRepository(repository)).requestCache(c->c.requestCache(new NullRequestCache()))
            .authorizeHttpRequests(a->a.requestMatchers(r->"POST".equals(r.getMethod()) && "/api/v1/admin/auth/login".equals(r.getServletPath())).permitAll()
                .requestMatchers(r->"GET".equals(r.getMethod()) && "/api/v1/admin/account".equals(r.getServletPath()) || "POST".equals(r.getMethod()) && Set.of("/api/v1/admin/account/password","/api/v1/admin/account/google-link").contains(r.getServletPath()))
                .access((auth,context)->new AuthorizationDecision(security.authorize(auth.get(),context.getRequest())))
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->reject(s,401,"UNAUTHENTICATED"))
                .accessDeniedHandler((r,s,x)->{
                    var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    boolean invalid=!new org.springframework.security.authentication.AuthenticationTrustResolverImpl().isAuthenticated(auth) || !(auth.getPrincipal() instanceof br.com.concursosimulator.identity.web.UserPrincipal);
                    int status=!(x instanceof org.springframework.security.web.csrf.CsrfException) && invalid?401:403;
                    reject(s,status,status==401?"UNAUTHENTICATED":"FORBIDDEN");
                }))
            .addFilterBefore(new NoStore(),org.springframework.web.filter.CorsFilter.class)
            .addFilterAfter(new Body(),AuthorizationFilter.class)
            .addFilterAfter(new LocalLogin(access,security),Body.class).build();
    }
    static void reject(HttpServletResponse s,int status,String code)throws IOException {
        s.setStatus(status);s.setContentType("application/json");s.setHeader("Cache-Control","no-store");
        if(status==429)s.setHeader("Retry-After","900");
        s.getWriter().write("{\"error\":\""+code+"\"}");
    }
    static class NoStore extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws IOException,ServletException {s.setHeader("Cache-Control","no-store");chain.doFilter(r,s);}
    }
    /** Bounded UTF-8, exact field grammar and duplicate detection, before either DTO consumer. */
    static class Body extends OncePerRequestFilter {
        private final JsonMapper json=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(2).maxStringLength(512).build()).build()).build();
        @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws IOException,ServletException {
            if(!"POST".equals(r.getMethod())) {chain.doFilter(r,s);return;}
            if(r.getContentLengthLong()>8192){reject(s,413,"REQUEST_TOO_LARGE");return;}
            if(r.getHeader("Content-Encoding")!=null && !"identity".equalsIgnoreCase(r.getHeader("Content-Encoding"))){reject(s,415,"UNSUPPORTED_CONTENT_ENCODING");return;}
            var content=r.getContentType();
            if(content==null || !content.toLowerCase(Locale.ROOT).matches("application/json(?:\\s*;\\s*charset=utf-8)?")){reject(s,415,"UNSUPPORTED_MEDIA_TYPE");return;}
            var bytes=r.getInputStream().readNBytes(8193);if(bytes.length>8192){reject(s,413,"REQUEST_TOO_LARGE");return;}
            try {
                var text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                try(var parser=json.createParser(text)) {
                    if(parser.nextToken()==null)throw new IllegalArgumentException();
                    var tree=json.readTree(parser);
                    var fields=r.getServletPath().endsWith("/login")?Set.of("username","password"):r.getServletPath().endsWith("/password")?Set.of("currentPassword","newPassword"):Set.of("currentPassword");
                    if(parser.nextToken()!=null || !tree.isObject() || !tree.propertyNames().equals(fields))throw new IllegalArgumentException();
                    for(var name:fields)if(!tree.get(name).isString() || tree.get(name).asString().codePointCount(0,tree.get(name).asString().length())>128)throw new IllegalArgumentException();
                }
            }catch(RuntimeException | CharacterCodingException invalid){reject(s,400,"INVALID_REQUEST");return;}
            chain.doFilter(new HttpServletRequestWrapper(r) {
                @Override public ServletInputStream getInputStream() {
                    var input=new ByteArrayInputStream(bytes);
                    return new ServletInputStream() {
                        public int read(){return input.read();} public boolean isFinished(){return input.available()==0;}
                        public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                    };
                }
                @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
            },s);
        }
    }
    static class LocalLogin extends OncePerRequestFilter {
        private final AdministrativeAccessUseCases access;private final AdministrativeSecurity security;
        // Technical origin budget uses the actual socket peer, never an untrusted forwarded header.
        private final Map<String,Window> origins=new ConcurrentHashMap<>();
        private record Window(long start,int count) {}
        LocalLogin(AdministrativeAccessUseCases access,AdministrativeSecurity security){this.access=access;this.security=security;}
        public record LoginRequest(String username,String password) { @Override public String toString(){return "LoginRequest[redacted]";} }
        @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws IOException,ServletException {
            if(!"/api/v1/admin/auth/login".equals(r.getServletPath()) || !"POST".equals(r.getMethod())){chain.doFilter(r,s);return;}
            long now=Instant.now().getEpochSecond();
            if(origins.size()>4096)origins.entrySet().removeIf(e->now-e.getValue().start>=900);
            if(origins.size()>4096 && !origins.containsKey(r.getRemoteAddr())){reject(s,429,"LOGIN_THROTTLED");return;}
            var window=origins.compute(r.getRemoteAddr(),(k,v)->v==null || now-v.start>=900?new Window(now,1):new Window(v.start,v.count+1));
            if(window.count>30){reject(s,429,"LOGIN_THROTTLED");return;}
            try {
                var input=JsonMapper.builder().build().readValue(r.getInputStream(),LoginRequest.class);
                var a=access.login(input.username(),input.password());security.replace(r,s,a);s.setStatus(204);
            }catch(AdministrativeAccessUseCases.Failure failed) { reject(s,"LOGIN_THROTTLED".equals(failed.code())?429:401,failed.code()); }
            catch(org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException unavailable){reject(s,503,"ADMIN_UNAVAILABLE");}
            catch(RuntimeException failed){reject(s,500,"ADMIN_FAILURE");}
        }
    }
}

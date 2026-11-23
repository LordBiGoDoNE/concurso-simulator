package br.com.concursosimulator.mockexam.infrastructure;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Indisponibilidade de sessão ocorre antes do MVC; não deve virar 404, visitante ou sucesso. */
@Configuration
public class AttemptAvailabilityConfiguration {
    @Bean FilterRegistrationBean<OncePerRequestFilter> attemptAvailability(@org.springframework.beans.factory.annotation.Value("${app.cors-origins}")String configuredOrigins,
            br.com.concursosimulator.shared.infrastructure.http.ApiRoutes routes) {
        var origins=java.util.Arrays.stream(configuredOrigins.split(",")).map(String::trim).toList();
        var filter=new OncePerRequestFilter() {
            @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !routes.jsonFailures(request); }
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                response.setHeader("Cache-Control","no-store");
                try { chain.doFilter(request,response); }
                catch (RuntimeException | ServletException failure) {
                    if (response.isCommitted() || !databaseFailure(failure)) throw failure;
                    response.resetBuffer();response.setHeader("Content-Length",null);response.setStatus(503);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
                    var origin=request.getHeader("Origin");
                    if(origin!=null && origins.contains(origin)){response.setHeader("Access-Control-Allow-Origin",origin);response.setHeader("Access-Control-Allow-Credentials","true");response.addHeader("Vary","Origin");}
                    br.com.concursosimulator.shared.infrastructure.http.JsonRejection.write(response,503,"UNAVAILABLE");
                }
            }
        };
        var registration=new FilterRegistrationBean<OncePerRequestFilter>(filter);registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER-10);return registration;
    }
    private static boolean databaseFailure(Throwable failure) {
        return br.com.concursosimulator.shared.infrastructure.http.TransientFailures.transientCause(failure);
    }
}

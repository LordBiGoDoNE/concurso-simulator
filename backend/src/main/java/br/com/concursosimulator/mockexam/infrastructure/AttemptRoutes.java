package br.com.concursosimulator.mockexam.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

public final class AttemptRoutes {
    private AttemptRoutes() {}
    public static boolean owns(HttpServletRequest r) {
        return r.getServletPath().equals("/api/v1/attempts") || r.getServletPath().startsWith("/api/v1/attempts/")
                || r.getServletPath().equals("/api/v1/me/attempts");
    }
    public static boolean permits(HttpServletRequest r) {
        var p=r.getServletPath();
        return switch (r.getMethod()) {
            case "GET" -> p.matches("/api/v1/attempts/[^/]+(/result)?");
            case "POST" -> p.equals("/api/v1/attempts") || p.matches("/api/v1/attempts/[^/]+/finish");
            case "PUT" -> p.matches("/api/v1/attempts/[^/]+/answers/[^/]+");
            default -> false;
        };
    }
}

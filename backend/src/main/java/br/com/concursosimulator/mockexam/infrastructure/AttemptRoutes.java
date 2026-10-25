package br.com.concursosimulator.mockexam.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

public final class AttemptRoutes {
    private AttemptRoutes() {}
    public static boolean owns(HttpServletRequest r) {
        return r.getServletPath().equals("/api/v1/attempts") || r.getServletPath().startsWith("/api/v1/attempts/")
                || r.getServletPath().equals("/api/v1/me/attempts")
                || r.getServletPath().equals("/api/v1/lesson-practices") || r.getServletPath().startsWith("/api/v1/lesson-practices/")
                || r.getServletPath().equals("/api/v1/me/lesson-practices");
    }
    public static boolean permits(HttpServletRequest r) {
        var p=r.getServletPath();
        return switch (r.getMethod()) {
            case "GET" -> p.matches("/api/v1/(attempts|lesson-practices)/[^/]+(/result)?");
            case "POST" -> (p.equals("/api/v1/attempts") || p.equals("/api/v1/lesson-practices")) || p.matches("/api/v1/(attempts|lesson-practices)/[^/]+/finish");
            case "PUT" -> p.matches("/api/v1/(attempts|lesson-practices)/[^/]+/answers/[^/]+");
            default -> false;
        };
    }
}

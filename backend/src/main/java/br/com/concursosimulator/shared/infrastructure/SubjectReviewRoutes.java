package br.com.concursosimulator.shared.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

/** Reconhecimento técnico de rotas privadas; não decide identidade ou posse. */
public final class SubjectReviewRoutes {
    private SubjectReviewRoutes() {}
    public static boolean overview(HttpServletRequest r) { return r.getServletPath().equals("/api/v1/me/subject-review"); }
    public static boolean individual(HttpServletRequest r) { return r.getServletPath().matches("/api/v1/attempts/[^/]+/subject-review"); }
    public static boolean owns(HttpServletRequest r) { return overview(r) || individual(r); }
    public static boolean permits(HttpServletRequest r) { return "GET".equals(r.getMethod()) && owns(r); }
}

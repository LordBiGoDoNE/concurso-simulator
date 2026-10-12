package br.com.concursosimulator.shared.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

/** Mesmo matcher da cadeia stateless e do bypass de sessão, inclusive negações dentro dessa fronteira. */
public final class PublicQuestionRoutes {
    private PublicQuestionRoutes() {}
    public static boolean owns(HttpServletRequest request) {
        var path=request.getServletPath(); return path.equals("/api/v1/question-bank") || path.startsWith("/api/v1/question-bank/");
    }
    public static boolean permits(HttpServletRequest request) {
        var path=request.getServletPath();
        return request.getMethod().equals("GET") && (path.equals("/api/v1/question-bank/catalog") || path.equals("/api/v1/question-bank/questions")
                || path.matches("/api/v1/question-bank/questions/[^/]+"));
    }
}

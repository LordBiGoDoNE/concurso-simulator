package br.com.concursosimulator.shared.infrastructure.http;

import java.util.Objects;

/**
 * Rota HTTP declarada: método, padrão ({@code PathPattern}) e acesso técnico exigido antes do controller.
 *
 * @param passwordChangeExempt se o caminho continua acessível à conta com troca de senha pendente
 */
public record ApiRoute(String method, String pattern, Access access, boolean passwordChangeExempt) {
    public enum Access {
        /** Qualquer requisição; nenhuma identidade é exigida. */
        PUBLIC,
        /** Visitante com sessão ou conta verificada, resolvidos pela segurança do simulado. */
        VISITOR_OR_ACCOUNT,
        /** Somente conta verificada, resolvida pela segurança do simulado. */
        ACCOUNT,
        /** Somente autenticação não anônima com {@code UserPrincipal}. */
        ACCOUNT_PRINCIPAL,
        /** Decidido pela cadeia própria da área administrativa. */
        ADMINISTRATIVE
    }

    public ApiRoute {
        Objects.requireNonNull(method); Objects.requireNonNull(pattern); Objects.requireNonNull(access);
        if (!pattern.startsWith("/")) throw new IllegalArgumentException("Padrão relativo: " + pattern);
    }

    public static ApiRoute get(String pattern, Access access) { return new ApiRoute("GET", pattern, access, false); }
    public static ApiRoute post(String pattern, Access access) { return new ApiRoute("POST", pattern, access, false); }
    public static ApiRoute put(String pattern, Access access) { return new ApiRoute("PUT", pattern, access, false); }

    /** O caminho segue acessível durante a troca de senha pendente, qualquer que seja o método. */
    public ApiRoute exemptFromPasswordChange() { return new ApiRoute(method, pattern, access, true); }

    public boolean requiresIdentity() {
        return access == Access.VISITOR_OR_ACCOUNT || access == Access.ACCOUNT || access == Access.ACCOUNT_PRINCIPAL;
    }
}

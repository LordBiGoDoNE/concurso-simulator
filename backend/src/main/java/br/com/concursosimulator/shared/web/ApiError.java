package br.com.concursosimulator.shared.web;

/** Corpo de erro da API: código estável em {@code UPPER_SNAKE_CASE}, sem mensagem de exceção. */
public record ApiError(String error) {
    public ApiError {
        ApiErrors.requireCode(error);
    }
}

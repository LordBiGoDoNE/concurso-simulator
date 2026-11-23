package br.com.concursosimulator.shared.web;

import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

/** Respostas de erro dos adaptadores web: status, {@code no-store} e corpo tipado. */
public final class ApiErrors {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]*");
    private ApiErrors() {}

    public static ResponseEntity<ApiError> of(int status, String code) {
        return with(status, new ApiError(code));
    }

    public static <T> ResponseEntity<T> with(int status, T body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }

    public static String requireCode(String code) {
        if (code == null || !CODE.matcher(code).matches()) throw new IllegalStateException("Código de erro fora do padrão: " + code);
        return code;
    }
}

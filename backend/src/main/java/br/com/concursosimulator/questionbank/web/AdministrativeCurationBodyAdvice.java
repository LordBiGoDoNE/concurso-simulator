package br.com.concursosimulator.questionbank.web;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

/**
 * Corpo JSON da curadoria administrativa: até 2 MiB, UTF-8, sem chaves duplicadas, campos
 * desconhecidos, conteúdo após o objeto ou coerção de escalares. Mesma política da importação.
 */
@ControllerAdvice(assignableTypes = {AdministrativeQuestionController.class, EditorialDecisionBatchController.class})
public class AdministrativeCurationBodyAdvice extends RequestBodyAdviceAdapter {
    static final int LIMIT = 2_097_152;
    private final JsonMapper strict = JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(LIMIT).build()).build())
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .withCoercionConfig(LogicalType.Textual, c -> c.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail).setCoercion(CoercionInputShape.Float, CoercionAction.Fail)).build();
    public static final class TooLarge extends RuntimeException {}
    @Override public boolean supports(MethodParameter parameter, Type target, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type type,
            Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        var bytes = input.getBody().readNBytes(LIMIT + 1);
        if (bytes.length > LIMIT) throw new TooLarge();
        try {
            var text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            var root = strict.readTree(text);
            if (root == null || !root.isObject()) throw new IllegalArgumentException();
            strict.treeToValue(root, parameter.getParameterType());
        } catch (RuntimeException | CharacterCodingException error) {
            throw new HttpMessageNotReadableException("INVALID_REQUEST", input);
        }
        return new HttpInputMessage() {
            public InputStream getBody() { return new ByteArrayInputStream(bytes); }
            public HttpHeaders getHeaders() { return input.getHeaders(); }
        };
    }
}

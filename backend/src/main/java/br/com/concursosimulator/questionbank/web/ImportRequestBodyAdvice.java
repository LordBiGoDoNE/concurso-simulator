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
import tools.jackson.databind.type.LogicalType;
import br.com.concursosimulator.questionbank.application.port.QuestionImportReader;
import tools.jackson.databind.json.JsonMapper;

/** Limita o stream e confere a gramática antes da conversão em DTO tipado. */
@ControllerAdvice(assignableTypes=AdministrativeImportController.class)
public class ImportRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private final JsonMapper strict=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(16_777_216).build()).build())
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(tools.jackson.databind.cfg.EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .withCoercionConfig(LogicalType.Textual,c->c.setCoercion(CoercionInputShape.Integer,CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean,CoercionAction.Fail).setCoercion(CoercionInputShape.Float,CoercionAction.Fail)).build();
    public static final class TooLarge extends RuntimeException {}
    @Override public boolean supports(MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter){return true;}
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input,MethodParameter parameter,Type type,Class<? extends HttpMessageConverter<?>> converter)throws IOException {
        var bytes=input.getBody().readNBytes(16_777_217);
        if(bytes.length>16_777_216)throw new TooLarge();
        try {
            if(input.getHeaders().getContentType()!=null && input.getHeaders().getContentType().getCharset()!=null
                    && !input.getHeaders().getContentType().getCharset().equals(StandardCharsets.UTF_8))throw new IllegalArgumentException();
            var text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            try(var parser=strict.createParser(text)){
                if(parser.nextToken()==null)throw new IllegalArgumentException();
                var root=strict.readTree(parser);if(parser.nextToken()!=null)throw new IllegalArgumentException();
                boolean apply=parameter.getParameterType()==AdministrativeImportController.ApplyRequest.class;
                var names=apply?java.util.Set.of("package","keys","confirmationToken"):java.util.Set.of("package","keys");
                if(!root.isObject() || !root.propertyNames().equals(names))throw new IllegalArgumentException();
                if(!root.get("keys").isArray() || apply && !root.get("confirmationToken").isString())throw new IllegalArgumentException();
                for(var key:root.get("keys"))if(!key.isString())throw new IllegalArgumentException();
                try{strict.treeToValue(root.get("package"),QuestionImportReader.Document.class);}
                catch(RuntimeException e){throw new QuestionImportReader.InvalidPackage();}
                // Apply the same strict textual coercion policy to keys and confirmation, not only package.
                strict.treeToValue(root,parameter.getParameterType());
            }
        }catch(tools.jackson.core.exc.StreamConstraintsException error){throw new QuestionImportReader.InvalidPackage();}
        catch(QuestionImportReader.InvalidPackage error){throw error;}
        catch(RuntimeException | CharacterCodingException error){throw new HttpMessageNotReadableException("INVALID_REQUEST",input);}
        return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(bytes);}public HttpHeaders getHeaders(){return input.getHeaders();}};
    }
}

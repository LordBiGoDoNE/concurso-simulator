package br.com.concursosimulator.mockexam.web;

import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.*;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Entrega ao controller o {@link AttemptOwner} que a segurança já resolveu e gravou na requisição.
 * A chave é o nome da classe do domínio, a mesma usada pela infraestrutura de segurança, sem que
 * o adaptador web dependa dela.
 */
@Configuration
public class CurrentActorResolver implements WebMvcConfigurer {
    static final String ACTOR = AttemptOwner.class.getName();

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(CurrentActor.class) && p.getParameterType() == AttemptOwner.class;
            }

            public Object resolveArgument(
                    MethodParameter p, ModelAndViewContainer m, NativeWebRequest request, WebDataBinderFactory b) {
                var actor = request.getAttribute(ACTOR, RequestAttributes.SCOPE_REQUEST);
                if (!(actor instanceof AttemptOwner))
                    throw new IllegalStateException("Ator técnico não resolvido pela segurança");
                return actor;
            }
        });
    }
}

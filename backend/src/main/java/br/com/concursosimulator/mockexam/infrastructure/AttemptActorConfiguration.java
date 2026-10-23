package br.com.concursosimulator.mockexam.infrastructure;

import br.com.concursosimulator.mockexam.domain.AttemptOwner;
import br.com.concursosimulator.mockexam.web.CurrentActor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.*;
import org.springframework.web.method.support.*;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
public class AttemptActorConfiguration implements WebMvcConfigurer {
    @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter p) {
                return p.hasParameterAnnotation(CurrentActor.class) && p.getParameterType()==AttemptOwner.class;
            }
            public Object resolveArgument(MethodParameter p,ModelAndViewContainer m,NativeWebRequest request,WebDataBinderFactory b) {
                var actor=request.getAttribute(AttemptAccess.ACTOR,RequestAttributes.SCOPE_REQUEST);
                if (!(actor instanceof AttemptOwner)) throw new IllegalStateException("Ator técnico não resolvido pela segurança");
                return actor;
            }
        });
    }
}

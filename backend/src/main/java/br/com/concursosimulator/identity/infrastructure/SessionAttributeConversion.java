package br.com.concursosimulator.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/**
 * Conversão dos atributos do Spring Session JDBC com a serialização Java padrão, exceto por um caso:
 * atributo gravado com uma classe que não existe mais (ex.: {@code UserPrincipal} antes de mudar de pacote)
 * é tratado como ausente. A sessão segue sem contexto de segurança: a conta precisa entrar de novo e o
 * estudo continua como visitante. Nunca restaura a identidade anterior por outro caminho.
 *
 * <p>Qualquer outra falha de desserialização (bytes corrompidos, versão de classe incompatível) continua
 * propagando: não se finge uma sessão válida nem se esconde dado inconsistente.
 */
@Configuration
public class SessionAttributeConversion {
    private static final Logger LOG = LoggerFactory.getLogger(SessionAttributeConversion.class);

    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> tolerantSessionAttributes() {
        return repository ->
                repository.setConversionService(conversionService(SessionAttributeConversion.class.getClassLoader()));
    }

    static GenericConversionService conversionService(ClassLoader classLoader) {
        var service = new GenericConversionService();
        service.addConverter(Object.class, byte[].class, new SerializingConverter());
        service.addConverter(
                byte[].class, Object.class, new AbsentWhenClassIsGone(new DeserializingConverter(classLoader)));
        return service;
    }

    static final class AbsentWhenClassIsGone implements Converter<byte[], Object> {
        private final DeserializingConverter delegate;

        AbsentWhenClassIsGone(DeserializingConverter delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object convert(byte[] source) {
            try {
                return delegate.convert(source);
            } catch (SerializationFailedException failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (cause instanceof ClassNotFoundException) {
                        // Sem nome de classe nem conteúdo: o log não revela o que a sessão guardava.
                        LOG.warn("Atributo de sessão com classe removida tratado como ausente");
                        return null;
                    }
                }
                throw failure;
            }
        }
    }
}

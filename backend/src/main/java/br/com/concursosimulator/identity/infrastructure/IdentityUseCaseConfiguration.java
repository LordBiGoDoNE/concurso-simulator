package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.ResolveExternalIdentityUseCase;
import br.com.concursosimulator.identity.application.port.IdentityRepository;
import br.com.concursosimulator.shared.infrastructure.SpringUnitOfWork;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class IdentityUseCaseConfiguration {
    @Bean
    ResolveExternalIdentityUseCase resolveExternalIdentity(IdentityRepository repository, PlatformTransactionManager manager) {
        return new ResolveExternalIdentityUseCase(repository, new SpringUnitOfWork(manager));
    }
}

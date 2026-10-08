package br.com.concursosimulator.shared.infrastructure;

import br.com.concursosimulator.shared.application.port.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class SpringUnitOfWork implements UnitOfWork {
    private final TransactionTemplate transaction;

    public SpringUnitOfWork(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T independently(Supplier<T> operation) {
        return transaction.execute(status -> operation.get());
    }
}

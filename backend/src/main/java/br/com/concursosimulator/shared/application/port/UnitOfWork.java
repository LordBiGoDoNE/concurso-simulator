package br.com.concursosimulator.shared.application.port;

import java.util.function.Supplier;

public interface UnitOfWork {
    /** Retornar após commit; ao falhar, completar rollback antes de lançar. */
    <T> T independently(Supplier<T> operation);
}

package br.com.concursosimulator.mockexam.application;

import br.com.concursosimulator.mockexam.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** A implementação confirma commit/rollback antes de retornar/traduzir concorrência. */
public interface AttemptStore {
    <T> T transaction(Function<Session,T> work);
    interface Session {
        Optional<Attempt.Snapshot> find(UUID id);
        Optional<Command> command(AttemptOwner owner, UUID operation);
        List<AttemptQuestion> eligible();
        void insert(Attempt.Snapshot snapshot);
        void update(Attempt.Snapshot snapshot, long expectedVersion);
        void record(AttemptOwner owner, UUID operation, Command command);
        List<Attempt.Snapshot> history(AttemptOwner owner, HistoryQuery query);
        int deleteExpired(Instant now);
    }
    record Command(String hash, Attempt.Snapshot outcome) {}
    record HistoryQuery(int page, int size, Boolean finished) {
        public HistoryQuery {
            if (page < 0 || page > 100_000 || size < 1 || size > 100) throw new IllegalArgumentException("Página inválida");
        }
    }
    final class ConcurrentWrite extends RuntimeException {
        public ConcurrentWrite(Throwable cause) { super(cause); }
    }
}

package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.application.AttemptStore;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.application.port.EligibleRevisionPool;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Repository
public class JpaAttemptStore implements AttemptStore {
    private final EntityManager entities;
    private final EligibleRevisionPool pool;
    private final TransactionTemplate transactions;
    private final JsonMapper json=JsonMapper.builder().build();
    public JpaAttemptStore(EntityManager entities, PlatformTransactionManager manager, EligibleRevisionPool pool) {
        this.entities=entities; this.pool=pool; transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transactions.setTimeout(15);
    }
    public <T> T transaction(Function<Session,T> work) {
        try {
            return transactions.execute(status -> {
                var session=new JpaSession();
                try { var result=work.apply(session); entities.flush(); return result; }
                finally { session.active=false; }
            });
        } catch (RuntimeException failure) {
            // execute concluiu rollback, inclusive quando o erro ocorreu no flush/commit.
            for (Throwable cause=failure;cause!=null;cause=cause.getCause()) {
                if (cause instanceof OptimisticLockException || cause instanceof org.hibernate.StaleObjectStateException
                        || cause instanceof java.sql.SQLException sql && "40001".equals(sql.getSQLState())
                        || cause instanceof ConstraintViolationException c && "me_command_operation_key".equals(c.getConstraintName()))
                    throw new ConcurrentWrite(failure);
            }
            throw failure;
        }
    }
    private final class JpaSession implements Session {
        private boolean active=true;
        private void active() { if (!active) throw new IllegalStateException("Transação encerrada"); }
        public Optional<Attempt.Snapshot> find(UUID id) {
            active(); return Optional.ofNullable(entities.find(AttemptRow.class,id)).map(JpaAttemptStore.this::read);
        }
        public Optional<Command> command(AttemptOwner owner, UUID operation) {
            active();
            return entities.createQuery("select c from CommandRow c where c.ownerKind=:kind and c.ownerId=:owner and c.operationId=:operation",CommandRow.class)
                    .setParameter("kind",owner.kind().name()).setParameter("owner",owner.id()).setParameter("operation",operation)
                    .getResultStream().findFirst().map(row -> {
                        var snapshot=decode(row.outcome);
                        require(snapshot.owner().equals(owner) && snapshot.id().equals(row.attemptId));
                        return new Command(row.payloadHash,snapshot);
                    });
        }
        public List<AttemptQuestion> eligible() {
            active(); return pool.capture().stream().map(e -> new AttemptQuestion(e.questionId(),e.revisionId(),e.fingerprint(),e.content())).toList();
        }
        public void insert(Attempt.Snapshot snapshot) {
            active(); var attempt=Attempt.restore(snapshot); require(snapshot.version()==0 && snapshot.finishedAt()==null && snapshot.answers().isEmpty());
            var row=new AttemptRow(); row.id=snapshot.id(); row.ownerKind=snapshot.owner().kind().name(); row.ownerId=snapshot.owner().id();
            row.accountId=snapshot.owner().kind()==AttemptOwner.Kind.ACCOUNT?snapshot.owner().id():null;
            row.createdAt=snapshot.createdAt(); row.expiresAt=attempt.expiresAt(); row.payload=json.writeValueAsString(snapshot);
            entities.persist(row); entities.flush(); // FK dos filhos independe da ordenação de inserts configurada.
            for (int i=0;i<snapshot.items().size();i++) {
                var item=snapshot.items().get(i); var r=new ItemRow(); r.id=item.id(); r.attemptId=snapshot.id(); r.position=i;
                r.questionId=item.question().questionId(); r.revisionId=item.question().revisionId(); r.payload=json.writeValueAsString(item);
                entities.persist(r);
            }
        }
        public void update(Attempt.Snapshot snapshot, long expectedVersion) {
            active(); var row=entities.find(AttemptRow.class,snapshot.id());
            if (row==null || row.version!=expectedVersion) throw new ConcurrentWrite(null);
            var old=read(row); Attempt.restore(snapshot);
            require(snapshot.version()==Math.addExact(expectedVersion,1) && old.finishedAt()==null
                    && old.owner().equals(snapshot.owner()) && old.createdAt().equals(snapshot.createdAt())
                    && old.composition().equals(snapshot.composition()) && old.items().equals(snapshot.items()));
            row.payload=json.writeValueAsString(snapshot); row.finishedAt=snapshot.finishedAt();
            // Hibernate emite UPDATE ... WHERE id=? AND version=?, incrementando @Version.
        }
        public void record(AttemptOwner owner, UUID operation, Command command) {
            active(); require(owner.equals(command.outcome().owner()));
            var row=new CommandRow(); row.id=UUID.randomUUID(); row.ownerKind=owner.kind().name(); row.ownerId=owner.id();
            row.operationId=operation; row.attemptId=command.outcome().id(); row.payloadHash=command.hash();
            row.outcome=json.writeValueAsString(command.outcome()); entities.persist(row);
        }
        public List<Attempt.Snapshot> history(AttemptOwner owner, HistoryQuery query) {
            active(); if (owner.kind()!=AttemptOwner.Kind.ACCOUNT) throw new Attempt.Unavailable();
            var suffix=query.finished()==null?"":query.finished()?" and a.finishedAt is not null":" and a.finishedAt is null";
            return entities.createQuery("select a from AttemptRow a where a.ownerKind='ACCOUNT' and a.ownerId=:owner"+suffix+" order by a.createdAt desc,a.id desc",AttemptRow.class)
                    .setParameter("owner",owner.id()).setFirstResult(Math.multiplyExact(query.page(),query.size())).setMaxResults(query.size())
                    .getResultList().stream().map(JpaAttemptStore.this::read).toList();
        }
        public int deleteExpired(Instant now) {
            active(); entities.flush();
            return entities.createNativeQuery("delete from me_attempt where owner_kind='VISITOR' and expires_at<=:now")
                    .setParameter("now",now).executeUpdate();
        }
    }
    private Attempt.Snapshot decode(String payload) { return Attempt.restore(json.readValue(payload,Attempt.Snapshot.class)).snapshot(); }
    private Attempt.Snapshot read(AttemptRow row) {
        var s=decode(row.payload); var a=Attempt.restore(s);
        require(s.id().equals(row.id) && s.owner().kind().name().equals(row.ownerKind) && s.owner().id().equals(row.ownerId)
                && Objects.equals(row.accountId,s.owner().kind()==AttemptOwner.Kind.ACCOUNT?s.owner().id():null)
                && s.version()==row.version && s.createdAt().equals(row.createdAt) && Objects.equals(s.finishedAt(),row.finishedAt)
                && Objects.equals(a.expiresAt(),row.expiresAt));
        var items=entities.createQuery("select i from ItemRow i where i.attemptId=:id order by i.position",ItemRow.class)
                .setParameter("id",row.id).getResultList();
        require(items.size()==s.items().size());
        for (int i=0;i<items.size();i++) {
            var r=items.get(i); var item=json.readValue(r.payload,Attempt.Item.class);
            require(r.position==i && r.id.equals(item.id()) && r.questionId.equals(item.question().questionId())
                    && r.revisionId.equals(item.question().revisionId()) && item.equals(s.items().get(i)));
        }
        return s;
    }
    private static void require(boolean condition) { if (!condition) throw new IllegalStateException("Persistência da tentativa inconsistente"); }
}

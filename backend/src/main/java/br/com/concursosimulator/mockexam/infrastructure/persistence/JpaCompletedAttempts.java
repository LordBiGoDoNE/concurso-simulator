package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.application.CompletedAttemptReader;
import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.TargetRole;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;

@Repository
public class JpaCompletedAttempts implements CompletedAttemptReader {
    private final EntityManager entities;
    private final TransactionTemplate read;
    public JpaCompletedAttempts(EntityManager entities,PlatformTransactionManager manager) {
        this.entities=entities;read=new TransactionTemplate(manager);
        read.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        read.setReadOnly(true);read.setTimeout(15);
    }
    @Override public Window recent(AttemptOwner account,TargetRole role,int limit,Instant now) {
        Objects.requireNonNull(account);Objects.requireNonNull(role);Objects.requireNonNull(now);
        if(account.kind()!=AttemptOwner.Kind.ACCOUNT)throw new Attempt.Unavailable();
        if(limit<1 || limit>MAX_LIMIT)throw new IllegalArgumentException("Limite de leitura inválido");
        return read.execute(status -> {
            // SQL nativo: posse/cargo e criação acessível no instante capturado ANTES de ordenar/limitar.
            @SuppressWarnings("unchecked") List<UUID> headers=entities.createNativeQuery(
                    "select id from me_attempt where owner_kind='ACCOUNT' and owner_id=:owner and created_at<=:now and finished_at is not null "
                    +"and payload->'composition'->>'role'=:role order by finished_at desc,id desc",UUID.class)
                    .setParameter("owner",account.id()).setParameter("now",now).setParameter("role",role.name()).setMaxResults(limit+1).getResultList();
            var ids=headers.stream().limit(limit).toList();
            if(ids.isEmpty())return new Window(List.of(),false);
            var rows=entities.createQuery("select a from AttemptRow a where a.id in :ids",AttemptRow.class)
                    .setParameter("ids",ids).getResultList();
            var byId=new HashMap<UUID,AttemptRow>();for(var row:rows)byId.put(row.id,row);
            var ordered=ids.stream().map(id -> {
                var row=byId.get(id);if(row==null)throw new IllegalStateException("Cabeçalho sem tentativa");return row;
            }).toList();
            var snapshots=new AttemptSnapshotReader(entities).read(ordered);
            var evidence=snapshots.stream().map(s -> {
                if(s.composition().role()!=role)throw new IllegalStateException("Cargo da captura divergente");
                return Attempt.restore(s).evidence(account,now);
            }).toList();
            return new Window(evidence,headers.size()>limit);
        });
    }
    @Override public CompletedAttempt one(AttemptOwner actor,UUID id,Instant now) {
        Objects.requireNonNull(actor);Objects.requireNonNull(id);Objects.requireNonNull(now);
        return read.execute(status -> {
            var row=entities.createQuery("select a from AttemptRow a where a.id=:id and a.ownerKind=:kind and a.ownerId=:owner "
                    +"and a.createdAt<=:now and (a.expiresAt is null or a.expiresAt>:now)",AttemptRow.class)
                    .setParameter("id",id).setParameter("kind",actor.kind().name()).setParameter("owner",actor.id())
                    .setParameter("now",now).getResultStream().findFirst().orElseThrow(Attempt.Unavailable::new);
            var snapshot=new AttemptSnapshotReader(entities).read(List.of(row)).getFirst();
            return Attempt.restore(snapshot).evidence(actor,now);
        });
    }
}

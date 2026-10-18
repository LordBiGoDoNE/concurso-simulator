package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.domain.*;
import jakarta.persistence.EntityManager;
import java.util.*;

/** Uma validaÃ§Ã£o para leituras individuais e em lote; nenhuma consulta por item. */
final class AttemptSnapshotReader {
    private final EntityManager entities;
    private final AttemptCodec codec=new AttemptCodec();
    AttemptSnapshotReader(EntityManager entities) { this.entities=entities; }
    List<Attempt.Snapshot> read(List<AttemptRow> rows) {
        if(rows.isEmpty())return List.of();
        var snapshots=new LinkedHashMap<UUID,Attempt.Snapshot>();
        for(var row:rows) {
            var s=decode(row.payload);var a=Attempt.restore(s);
            require(snapshots.put(row.id,s)==null && s.id().equals(row.id)
                    && s.owner().kind().name().equals(row.ownerKind) && s.owner().id().equals(row.ownerId)
                    && Objects.equals(row.accountId,s.owner().kind()==AttemptOwner.Kind.ACCOUNT?s.owner().id():null)
                    && s.version()==row.version && s.createdAt().equals(row.createdAt)
                    && Objects.equals(s.finishedAt(),row.finishedAt) && Objects.equals(a.expiresAt(),row.expiresAt));
        }
        var ids=List.copyOf(snapshots.keySet());
        var stored=entities.createQuery("select i from ItemRow i where i.attemptId in :ids order by i.attemptId,i.position",ItemRow.class)
                .setParameter("ids",ids).getResultList();
        var grouped=new HashMap<UUID,List<ItemRow>>();
        for(var item:stored)grouped.computeIfAbsent(item.attemptId,k -> new ArrayList<>()).add(item);
        var captures=new HashMap<UUID,String>();
        if(snapshots.values().stream().anyMatch(s -> s.format()==2)) {
            @SuppressWarnings("unchecked") List<Object[]> instances=entities.createNativeQuery(
                    "select p.item_id,p.payload::text from pq_instance p join me_item i on i.id=p.item_id where i.attempt_id in :ids")
                    .setParameter("ids",ids).getResultList();
            for(var p:instances)require(captures.put((UUID)p[0],(String)p[1])==null);
        }
        for(var s:snapshots.values()) {
            var items=grouped.getOrDefault(s.id(),List.of());require(items.size()==s.items().size());
            for(int i=0;i<items.size();i++) {
                var row=items.get(i);var item=decodeItem(row.payload,s.format());
                require(row.position==i && row.id.equals(item.id()) && row.questionId.equals(item.question().questionId())
                        && row.revisionId.equals(item.question().revisionId()) && item.equals(s.items().get(i)));
                var captured=captures.get(row.id);
                require((captured!=null)==(item.question().source().capture()!=null));
                if(captured!=null)require(decodeItem(captured,2).equals(item));
            }
        }
        return List.copyOf(snapshots.values());
    }
    private Attempt.Snapshot decode(String payload) {
        try { return codec.decode(payload); }
        catch(RuntimeException invalid) { throw new IllegalStateException("Snapshot persistido inconsistente",invalid); }
    }
    private Attempt.Item decodeItem(String payload,int format) {
        try { return codec.item(payload,format); }
        catch(RuntimeException invalid) { throw new IllegalStateException("Item persistido inconsistente",invalid); }
    }
    private static void require(boolean valid) {
        if(!valid)throw new IllegalStateException("PersistÃªncia da tentativa inconsistente");
    }
}

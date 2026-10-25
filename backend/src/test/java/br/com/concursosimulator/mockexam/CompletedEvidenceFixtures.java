package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** Dados sintéticos de volume, construídos pelo domínio, sem decisões editoriais reais. */
final class CompletedEvidenceFixtures {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    static Attempt.Snapshot snapshot(AttemptOwner owner,TargetRole role,UUID id,Instant at,List<AttemptQuestion> questions,boolean finished) {
        var portions=new EnumMap<Discipline,Composition.Portion>(Discipline.class);
        for(var d:Discipline.values()) {
            int count=(int)questions.stream().filter(q -> q.content().classification().discipline()==d).count();
            if(count>0)portions.put(d,new Composition.Portion(count,Set.of()));
        }
        var items=questions.stream().map(q -> new Attempt.Item(UUID.randomUUID(),q,q.content().alternatives().stream().map(a->a.id()).toList(),
                q.source().capture()==null?Map.of():q.source().capture().parameterStrings())).toList();
        var a=Attempt.start(id,owner,new Composition(role,portions,questions.stream().anyMatch(q->q.source().capture()!=null)),at.minusSeconds(1),items);
        if(finished)a.finish(owner,at,0);return a.snapshot();
    }
    static AttemptQuestion question(AttemptDatabase db) {
        var q=db.seed("Evidência sintética",true);return new AttemptQuestion(q.id(),q.latest().id(),q.latest().fingerprint(),q.latest().content());
    }
    static void insert(AttemptDatabase db,List<Attempt.Snapshot> snapshots) {
        var parents=new ArrayList<Object[]>();var items=new ArrayList<Object[]>();
        for(var s:snapshots) {
            var expiry=Attempt.restore(s).expiresAt();
            parents.add(new Object[]{s.id(),s.owner().kind().name(),s.owner().id(),s.owner().kind()==AttemptOwner.Kind.ACCOUNT?s.owner().id():null,
                    Timestamp.from(s.createdAt()),s.finishedAt()==null?null:Timestamp.from(s.finishedAt()),expiry==null?null:Timestamp.from(expiry),s.version(),legacySnapshot(s)});
            for(int i=0;i<s.items().size();i++) {
                var item=s.items().get(i);items.add(new Object[]{item.id(),s.id(),i,item.question().questionId(),item.question().revisionId(),legacyItem(item)});
            }
        }
        new TransactionTemplate(db.manager).execute(status -> {
            db.jdbc.batchUpdate("insert into me_attempt(id,owner_kind,owner_id,account_id,created_at,finished_at,expires_at,version,payload) values (?,?,?,?,?,?,?,?,?::jsonb)",parents);
            db.jdbc.batchUpdate("insert into me_item(id,attempt_id,position,question_id,revision_id,payload) values (?,?,?,?,?,?::jsonb)",items);return null;
        });
    }
    // Fixture do formato2 congelado: campos de modalidade pertencem somente ao3.
    private static String legacySnapshot(Attempt.Snapshot s) {
        var node=(tools.jackson.databind.node.ObjectNode)JSON.valueToTree(s);
        node.remove("purpose");node.remove("practiceTopic");
        for(var item:node.get("items"))((tools.jackson.databind.node.ObjectNode)item.get("question")).remove("purpose");
        return JSON.writeValueAsString(node);
    }
    private static String legacyItem(Attempt.Item item) {
        var node=(tools.jackson.databind.node.ObjectNode)JSON.valueToTree(item);
        ((tools.jackson.databind.node.ObjectNode)node.get("question")).remove("purpose");return JSON.writeValueAsString(node);
    }
    static List<List<String>> rows(AttemptDatabase db) {
        return List.of("app_user","qb_question","qb_revision","qb_decision","me_attempt","me_item","me_command","pq_instance")
                .stream().map(t -> db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
    }
}

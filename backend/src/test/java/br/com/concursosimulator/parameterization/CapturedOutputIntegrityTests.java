package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.parameterization.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.util.*;
import static br.com.concursosimulator.parameterization.ParameterizedPersistenceTests.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class CapturedOutputIntegrityTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @ParameterizedTest @ValueSource(strings={"7","8"})
    void validTupleDigestSwapIsRejectedAtV8CommitOrLegacyUpgrade(String target) {
        try(var db=new ParameterDatabase(PG,target)) {
            var v=db.approve(db.seed(Family.PERCENTAGE));var s=store(db);
            var q=s.transaction(tx->tx.eligible()).getFirst();
            var generated=AttemptMaterializer.materialize(List.of(q),List.of(v),new Random(){
                @Override public int nextInt(int bound){return 0;}
            },UUID::randomUUID).getFirst();
            var item=new Attempt.Item(UUID.randomUUID(),generated,generated.content().alternatives().stream().map(a->a.id()).toList(),
                    generated.source().capture().parameterStrings());
            var snapshot=Attempt.start(UUID.randomUUID(),VISITOR,composition(true),NOW,List.of(item)).snapshot();
            var other=v.definition().family().domain().get(1);var output=NumericGenerator.render(v.definition(),other);
            v.manifest().verifyOutput(output);
            var json=JsonMapper.builder().build();var payload=(ObjectNode)json.valueToTree(snapshot);
            var badItem=(ObjectNode)payload.get("items").get(0);var capture=(ObjectNode)badItem.get("question").get("source").get("capture");
            capture.set("parameters",json.valueToTree(other));capture.put("outputDigest",output.digest());
            var strings=new TreeMap<String,String>();other.values().forEach((k,val)->strings.put(k,val.toString()));
            badItem.set("parameters",json.valueToTree(strings));
            assertThat(json.treeToValue(json.valueToTree(snapshot),Attempt.Snapshot.class)).isEqualTo(snapshot);
            assertThatThrownBy(()->json.treeToValue(payload,Attempt.Snapshot.class)).isInstanceOf(RuntimeException.class);
            Runnable insert=()->new TransactionTemplate(db.manager).execute(status->{
                db.jdbc.update("insert into me_attempt(id,owner_kind,owner_id,created_at,expires_at,payload) values (?,'VISITOR',?,?,?,?::jsonb)",
                        snapshot.id(),VISITOR.id(),Timestamp.from(NOW),Timestamp.from(NOW.plusSeconds(86400)),payload.toString());
                db.jdbc.update("insert into me_item values (?,?,0,?,?,?::jsonb)",item.id(),snapshot.id(),q.questionId(),q.revisionId(),badItem.toString());
                db.jdbc.update("insert into pq_instance values (?,?,1,?::jsonb)",item.id(),v.definition().id(),badItem.toString());
                db.jdbc.update("insert into me_command values (?,'VISITOR',?,?,?,? ,?::jsonb)",UUID.randomUUID(),VISITOR.id(),UUID.randomUUID(),snapshot.id(),"a".repeat(64),payload.toString());
                return null;
            });
            if(target.equals("8")) {
                assertThatThrownBy(insert::run).isInstanceOf(RuntimeException.class);
                for(var table:List.of("me_attempt","me_item","pq_instance","me_command"))
                    assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isZero();
            } else {
                insert.run(); // Reproduz também a aceitação da falha no schema anterior.
                var before=db.jdbc.queryForObject("select payload::text from pq_instance",String.class);
                assertThatThrownBy(()->db.migrate("8")).isInstanceOf(RuntimeException.class);
                assertThat(db.jdbc.queryForObject("select payload::text from pq_instance",String.class)).isEqualTo(before);
                assertThat(db.jdbc.queryForObject("select count(*) from flyway_schema_history where version='8'",Integer.class)).isZero();
                for(var table:List.of("me_attempt","me_item","pq_instance","me_command"))
                    assertThat(db.jdbc.queryForObject("select count(*) from "+table,Integer.class)).isEqualTo(1);
            }
        }
    }
    @Test void all111FrozenOutputsMatchJavaAndPostgresDigestsAndCanBeRestored() {
        int checked=0;
        try(var db=new ParameterDatabase(PG)) {
            var s=store(db);var json=JsonMapper.builder().build();
            for(var family:Family.values()) {
                var v=db.approve(db.seed(family));
                var q=s.transaction(tx->tx.eligible()).stream().filter(base->base.questionId().equals(v.definition().baseQuestionId())).findFirst().orElseThrow();
                for(int index=0;index<family.domain().size();index++) {
                    final int selected=index;
                    var generated=AttemptMaterializer.materialize(List.of(q),List.of(v),new Random(){
                        @Override public int nextInt(int bound){return selected;}
                    },UUID::randomUUID).getFirst();
                    var capture=generated.source().capture();
                    var item=new Attempt.Item(UUID.randomUUID(),generated,generated.content().alternatives().stream().map(a->a.id()).toList(),capture.parameterStrings());
                    assertThat(CapturedOutputDigest.fromContent(capture.definitionDigest(),capture.parameters(),generated.content())).isEqualTo(capture.outputDigest());
                    assertThat(db.jdbc.queryForObject("select pq_captured_output_digest(?::jsonb)",String.class,json.writeValueAsString(item))).isEqualTo(capture.outputDigest());
                    var snapshot=Attempt.start(UUID.randomUUID(),VISITOR,composition(true),NOW,List.of(item)).snapshot();
                    s.transaction(tx->{tx.insert(snapshot);return null;});
                    var restored=s.transaction(tx->tx.find(snapshot.id()).orElseThrow());
                    assertThat(restored).isEqualTo(snapshot);
                    checked++;
                }
            }
            assertThat(checked).isEqualTo(111);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_instance",Integer.class)).isEqualTo(111);
        }
    }
    @Test void additiveV8PreservesV7SnapshotsCommandsAndChecksums() {
        try(var db=new ParameterDatabase(PG,"7")) {
            db.approve(db.seed(Family.SIMPLE_INTEREST));var s=store(db);var operation=UUID.randomUUID();
            var created=cases(s).create(VISITOR,operation,composition(true)).attempt();
            var tables=List.of("qb_question","qb_revision","qb_decision","pq_family","pq_version","pq_decision","me_attempt","me_item","pq_instance","me_command");
            var before=tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList();
            var checksums=db.jdbc.queryForList("select version,checksum from flyway_schema_history where success and version is not null order by installed_rank");
            db.factory.close();db.factory=null;db.migrate("8");db.open(true);
            assertThat(tables.stream().map(t->db.jdbc.queryForList("select row_to_json(r)::text from "+t+" r order by 1",String.class)).toList()).isEqualTo(before);
            assertThat(db.jdbc.queryForList("select version,checksum from flyway_schema_history where success and version is not null and version<>'8' order by installed_rank")).isEqualTo(checksums);
            var unavailable=new br.com.concursosimulator.mockexam.infrastructure.persistence.JpaAttemptStore(db.entities,db.manager,
                    ()->{throw new AssertionError("Sem banco atual");},()->{throw new AssertionError("Sem gerador atual");});
            assertThat(cases(unavailable).read(VISITOR,created.id())).isEqualTo(created);
            assertThat(cases(unavailable).create(VISITOR,operation,composition(true)).attempt()).isEqualTo(created);
            var item=unavailable.transaction(tx->tx.find(created.id()).orElseThrow()).items().getFirst();
            cases(unavailable).answer(VISITOR,created.id(),UUID.randomUUID(),0,item.id(),item.question().content().correctAlternativeId());
            assertThat(cases(unavailable).finish(VISITOR,created.id(),UUID.randomUUID(),1).result().correct()).isEqualTo(1);
        }
    }
}

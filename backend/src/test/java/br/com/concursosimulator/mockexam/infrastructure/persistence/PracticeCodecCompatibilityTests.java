package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.QuestionFixtures;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PracticeCodecCompatibilityTests {
    @ParameterizedTest @ValueSource(ints={3,4})
    void savingPreservesCapturedArraysEvenWhenTheCurrentJvmWouldSerializeSetsInAnotherOrder(int format) {
        var json=JsonMapper.builder().build();var codec=new AttemptCodec();
        var content=QuestionFixtures.content("Captura sintética com dois cargos editoriais");
        var question=new AttemptQuestion(UUID.randomUUID(),UUID.randomUUID(),content.fingerprint(),content,QuestionSource.fixed(),QuestionPurpose.PRATICA_AULA);
        var item=new Attempt.Item(UUID.randomUUID(),question,List.of(QuestionFixtures.FIRST,QuestionFixtures.SECOND),Map.of());
        var owner=new AttemptOwner(AttemptOwner.Kind.ACCOUNT,UUID.randomUUID());var now=Instant.parse("2026-11-11T12:00:00Z");
        var composition=new Composition(TargetRole.ADMINISTRATIVE_ASSISTANT,Map.of(Discipline.PORTUGUESE,new Composition.Portion(1,Set.of(Topic.P01))));
        var snapshot=Attempt.restore(new Attempt.Snapshot(UUID.randomUUID(),owner,format==3?composition:null,now,null,Attempt.POLICY,List.of(item),Map.of(),0,format,QuestionPurpose.PRATICA_AULA,Topic.P01)).snapshot();
        var original=(ObjectNode)json.readTree(codec.encode(snapshot,null));
        var roles=(ArrayNode)original.get("items").get(0).get("question").get("content").get("classification").get("roles");
        assertThat(roles.size()).isEqualTo(2);var first=roles.get(0).deepCopy();var second=roles.get(1).deepCopy();
        roles.removeAll();roles.add(second);roles.add(first);
        var frozen=json.writeValueAsString(original);var attempt=Attempt.restore(codec.decode(frozen));
        attempt.answer(owner,now,0,item.id(),QuestionFixtures.FIRST);
        var answered=codec.encode(attempt.snapshot(),frozen);
        assertThat(immutable(json,answered)).isEqualTo(immutable(json,frozen));
        assertThat(codec.decode(answered).answers()).containsEntry(item.id(),QuestionFixtures.FIRST);
        attempt.finish(owner,now,1);
        var finished=codec.encode(attempt.snapshot(),answered);
        assertThat(immutable(json,finished)).isEqualTo(immutable(json,frozen));
        assertThat(codec.decode(finished).finishedAt()).isEqualTo(now);
        assertThat(codec.decode(finished).version()).isEqualTo(2);
    }
    private ObjectNode immutable(JsonMapper json,String payload) {
        var node=(ObjectNode)json.readTree(payload);
        for(var field:List.of("answers","finishedAt","version"))node.remove(field);
        return node;
    }
}

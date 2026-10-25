package br.com.concursosimulator.mockexam;

import br.com.concursosimulator.mockexam.application.*;
import br.com.concursosimulator.mockexam.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import static br.com.concursosimulator.mockexam.AttemptFixtures.*;
import static org.assertj.core.api.Assertions.*;

class AttemptUseCasesTests {
    static final class Memory implements AttemptStore {
        Map<UUID,Attempt.Snapshot> attempts=new HashMap<>();
        Map<String,Command> commands=new HashMap<>();
        List<AttemptQuestion> pool=pool(5);
        List<br.com.concursosimulator.parameterization.domain.TemplateVersion> templates=List.of();
        boolean templatesUnavailable;
        int selections;
        RuntimeException failure;
        public <T> T transaction(Function<Session,T> work) {
            var a=new HashMap<>(attempts); var c=new HashMap<>(commands);
            var result=work.apply(new Session() {
                public Optional<Attempt.Snapshot> find(UUID id) { return Optional.ofNullable(a.get(id)); }
                public Optional<Command> command(AttemptOwner o, UUID op) { return Optional.ofNullable(c.get(o+":"+op)); }
                public List<AttemptQuestion> eligible() { selections++; return pool; }
                public List<br.com.concursosimulator.parameterization.domain.TemplateVersion> templates() {
                    if (templatesUnavailable) throw new IllegalStateException("Gerador/catálogo indisponível");
                    return templates;
                }
                public void insert(Attempt.Snapshot s) { a.put(s.id(),s); }
                public void update(Attempt.Snapshot s,long v) {
                    if (a.get(s.id()).version()!=v) throw new ConcurrentWrite(null); a.put(s.id(),s);
                }
                public void record(AttemptOwner o,UUID op,Command value) { c.put(o+":"+op,value); }
                public List<Attempt.Snapshot> history(AttemptOwner o,HistoryQuery q) {
                    return a.values().stream().filter(s -> s.owner().equals(o))
                            .filter(s -> s.purpose()==q.purpose())
                            .filter(s -> q.finished()==null || (s.finishedAt()!=null)==q.finished())
                            .sorted(Comparator.comparing(Attempt.Snapshot::createdAt).thenComparing(Attempt.Snapshot::id).reversed())
                            .skip((long)q.page()*q.size()).limit(q.size()).toList();
                }
                public int deleteExpired(Instant now) { throw new UnsupportedOperationException(); }
            });
            if (failure!=null) throw failure;
            attempts=a; commands=c; return result;
        }
    }
    AttemptUseCases cases(Memory store) { return cases(store,NOW); }
    AttemptUseCases cases(Memory store,Instant now) {
        return new AttemptUseCases(store,Clock.fixed(now,ZoneOffset.UTC),UUID::randomUUID,() -> new Random(42));
    }
    @Test void creationReplayKeepsSelectionAndOwnerAndRejectsChangedPayload() {
        var s=new Memory(); var uc=cases(s); var op=UUID.randomUUID();
        var first=uc.create(ACCOUNT,op,composition(3)); var retry=uc.create(ACCOUNT,op,composition(3));
        assertThat(retry.replayed()).isTrue(); assertThat(retry.attempt()).isEqualTo(first.attempt());
        assertThat(s.selections).isEqualTo(1); assertThat(s.attempts).hasSize(1);
        assertThatThrownBy(() -> uc.create(ACCOUNT,op,composition(2))).isInstanceOf(Attempt.Conflict.class);
        assertThat(uc.create(VISITOR,op,composition(3)).attempt().id()).isNotEqualTo(first.attempt().id());
    }
    @Test void commandRetryReturnsOriginalCommittedOutcomeEvenAfterLaterCommandsAndFinish() {
        var s=new Memory(); var uc=cases(s); var created=uc.create(ACCOUNT,UUID.randomUUID(),composition(3)).attempt();
        var item=created.items().getFirst(); var op=UUID.randomUUID();
        var answered=uc.answer(ACCOUNT,created.id(),op,0,item.id(),item.alternatives().getFirst().id());
        uc.answer(ACCOUNT,created.id(),UUID.randomUUID(),1,item.id(),null);
        var finishOp=UUID.randomUUID(); var finish=uc.finish(ACCOUNT,created.id(),finishOp,2);
        assertThat(uc.answer(ACCOUNT,created.id(),op,0,item.id(),item.alternatives().getFirst().id()).attempt()).isEqualTo(answered.attempt());
        assertThat(uc.finish(ACCOUNT,created.id(),finishOp,2).result()).isEqualTo(finish.result());
        assertThat(uc.finish(ACCOUNT,created.id(),finishOp,2).replayed()).isTrue();
        assertThat(uc.read(ACCOUNT,created.id()).version()).isEqualTo(3);
        assertThatThrownBy(() -> uc.answer(ACCOUNT,created.id(),op,0,item.id(),null)).isInstanceOf(Attempt.Conflict.class);
        assertThatThrownBy(() -> uc.finish(ACCOUNT,created.id(),UUID.randomUUID(),3)).isInstanceOf(Attempt.Conflict.class);
    }
    @Test void ownerCheckPrecedesReplayStateAndPayloadAndExpiryIsEnforced() {
        var s=new Memory(); var uc=cases(s); var op=UUID.randomUUID();
        var v=uc.create(VISITOR,op,composition(1)).attempt();
        assertThatThrownBy(() -> uc.read(ACCOUNT,v.id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> uc.answer(ACCOUNT,v.id(),op,99,UUID.randomUUID(),null)).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> uc.finish(ACCOUNT,v.id(),op,99)).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> uc.result(ACCOUNT,v.id())).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> cases(s,NOW.plusSeconds(86400)).create(VISITOR,op,composition(1))).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> cases(s,NOW.plusSeconds(86400)).read(VISITOR,v.id())).isInstanceOf(Attempt.Unavailable.class);
    }
    @Test void rollbackAndUnknownFailuresNeverClaimSuccessfulPersistence() {
        var s=new Memory(); var uc=cases(s); s.pool=List.of();
        assertThatThrownBy(() -> uc.create(ACCOUNT,UUID.randomUUID(),composition(1))).isInstanceOf(Composition.InsufficientPool.class);
        assertThat(s.attempts).isEmpty(); assertThat(s.commands).isEmpty();
        s.pool=pool(2); s.failure=new IllegalStateException("unavailable");
        assertThatThrownBy(() -> uc.create(ACCOUNT,UUID.randomUUID(),composition(1))).isSameAs(s.failure);
        assertThat(s.attempts).isEmpty(); assertThat(s.commands).isEmpty();
        s.failure=new AttemptStore.ConcurrentWrite(null);
        assertThatThrownBy(() -> uc.create(ACCOUNT,UUID.randomUUID(),composition(1))).isInstanceOf(Attempt.Conflict.class);
        assertThat(s.attempts).isEmpty();
    }
    @Test void historyIsAccountScopedLimitedOrderedAndOnlyIncludesFinalGrade() {
        var s=new Memory(); var uc=cases(s);
        var first=uc.create(ACCOUNT,UUID.randomUUID(),composition(1)).attempt();
        cases(s,NOW.plusSeconds(1)).create(ACCOUNT,UUID.randomUUID(),composition(2));
        uc.create(VISITOR,UUID.randomUUID(),composition(1)); uc.finish(ACCOUNT,first.id(),UUID.randomUUID(),0);
        var later=cases(s,NOW.plusSeconds(2));
        var page=later.history(ACCOUNT,new AttemptStore.HistoryQuery(0,1,null));
        assertThat(page).hasSize(1); assertThat(page.getFirst().percentage()).isNull();
        assertThat(later.history(ACCOUNT,new AttemptStore.HistoryQuery(1,1,null)).getFirst().id()).isEqualTo(first.id());
        assertThat(later.history(ACCOUNT,new AttemptStore.HistoryQuery(0,10,true))).singleElement().satisfies(e -> assertThat(e.percentage()).isEqualByComparingTo("0"));
        assertThatThrownBy(() -> uc.history(VISITOR,new AttemptStore.HistoryQuery(0,10,null))).isInstanceOf(Attempt.Unavailable.class);
        assertThatThrownBy(() -> new AttemptStore.HistoryQuery(0,101,null)).isInstanceOf(IllegalArgumentException.class);
    }
}

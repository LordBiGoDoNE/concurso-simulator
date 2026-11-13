package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.DecideQuestionUseCase;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.domain.EditorialDecision.ReviewKind;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static br.com.concursosimulator.questionbank.domain.EditorialDecision.Action.*;
import static org.assertj.core.api.Assertions.*;

/** Origem da revisão (#177): obrigatória em APPROVE novo; histórico sem o dado permanece não informado. */
@Testcontainers
class ReviewKindTests {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    static final Instant AT = Instant.parse("2026-11-12T00:00:00Z");
    static Question draft() {
        return Question.draft(UUID.randomUUID(), new QuestionOrigin("fixture", "review-kind-" + UUID.randomUUID()), UUID.randomUUID(), QuestionFixtures.content("Fixture origem"));
    }
    static EditorialDecision decision(Question q, EditorialDecision.Action action, ReviewKind kind) {
        return new EditorialDecision(action, q.id(), q.latest().id(), q.latest().fingerprint(), "Revisor sintético", AT,
                Set.of(EditorialDecision.Check.values()), "Fixture sintética", kind);
    }

    @Test void newApprovalRequiresReviewKindButOtherActionsDoNot() {
        var q = draft();
        q.decide(decision(q, SUBMIT, null));
        assertThatThrownBy(() -> q.decide(decision(q, APPROVE, null))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("origem");
        assertThat(q.eligible()).isEmpty(); assertThat(q.editorialSequence()).isEqualTo(1);
        q.decide(decision(q, APPROVE, ReviewKind.AI_ONLY));
        assertThat(q.eligible()).contains(q.latest());
        q.decide(decision(q, WITHDRAW, null));
        assertThat(q.decisions()).extracting(EditorialDecision::reviewKind).containsExactly(null, ReviewKind.AI_ONLY, null);
    }

    @Test void historicalApprovalWithoutReviewKindIsRestoredAsNotInformed() {
        var q = draft(); var submit = decision(q, SUBMIT, null); var legacy = decision(q, APPROVE, null);
        var restored = Question.restore(q.id(), q.origin(), q.revisions(), java.util.List.of(submit, legacy));
        assertThat(restored.eligible()).contains(q.latest());
        assertThat(restored.decisions().get(1).reviewKind()).isNull();
    }

    @Test void persistenceRoundTripsKindPreservesLegacyNullAndRejectsUnknownValues() {
        try (var db = new QuestionDatabase(POSTGRES)) {
            var decide = new DecideQuestionUseCase(db.store);
            var current = draft();
            db.store.transaction(s -> { s.insert(current); return null; });
            decide.execute(decision(current, SUBMIT, null), null, 0);
            decide.execute(decision(current, APPROVE, ReviewKind.HUMAN_PEER), null, 1);
            assertThat(db.jdbc.queryForList("select review_kind from qb_decision where question_id=? order by sequence", String.class, current.id()))
                    .containsExactly(null, "HUMAN_PEER");

            // Histórico gravado antes da V16: APPROVE sem origem, inserido diretamente como o legado faria.
            var legacy = draft();
            db.store.transaction(s -> { s.insert(legacy); return null; });
            var revision = legacy.latest();
            new TransactionTemplate(new DataSourceTransactionManager(db.jdbc.getDataSource())).executeWithoutResult(status -> {
                for (var row : new Object[][]{{0, "SUBMIT"}, {1, "APPROVE"}})
                    db.jdbc.update("insert into qb_decision(id,question_id,sequence,revision_id,action,fingerprint,responsible,decided_at,checklist,rationale) values (?,?,?,?,?,?,?,?,?,?)",
                            UUID.randomUUID(), legacy.id(), row[0], revision.id(), row[1], revision.fingerprint(), "Legado sintético",
                            java.sql.Timestamp.from(AT), "ACCESSIBILITY,CLARITY,CLASSIFICATION,CORRECTNESS,DISTRACTORS,SOURCES,STEP_BY_STEP", "Legado sintético");
                db.jdbc.update("update qb_revision set status='APPROVED' where id=?", revision.id());
                db.jdbc.update("update qb_question set eligible_revision_id=? where id=?", revision.id(), legacy.id());
            });
            var loaded = db.store.transaction(s -> s.loadForUpdate(legacy.id()).orElseThrow());
            assertThat(loaded.decisions().get(1).reviewKind()).isNull();
            assertThat(loaded.eligible()).contains(revision);
            // Nova decisão sobre questão com histórico legado continua funcionando e não reescreve o legado.
            decide.execute(decision(loaded, WITHDRAW, null), revision.id(), 2);
            assertThat(db.jdbc.queryForList("select review_kind from qb_decision where question_id=? order by sequence", String.class, legacy.id()))
                    .containsExactly(null, null, null);

            assertThatThrownBy(() -> db.jdbc.update("insert into qb_decision(id,question_id,sequence,revision_id,action,fingerprint,responsible,decided_at,checklist,rationale,review_kind) values (?,?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID(), legacy.id(), 3, revision.id(), "SUBMIT", revision.fingerprint(), "x", java.sql.Timestamp.from(AT), "", "x", "HUMAN"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> db.jdbc.update("update qb_decision set review_kind='AI_ONLY' where question_id=?", legacy.id()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}

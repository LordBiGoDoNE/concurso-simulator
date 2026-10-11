package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.domain.*;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Somente fixture sintética; não é material editorial real nem evidência de aprovação. */
public final class QuestionFixtures {
    private QuestionFixtures() {}
    public static final UUID FIRST = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID SECOND = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static QuestionContent content(String statement) {
        return new QuestionContent(new TextContent(statement), List.of(
                new QuestionContent.Alternative(FIRST, new TextContent("Opção sintética A")),
                new QuestionContent.Alternative(SECOND, new TextContent("Opção sintética B"))), FIRST,
                new TextContent("Resolução sintética: x² = x vezes x."),
                new QuestionClassification(Discipline.PORTUGUESE, Topic.P01, Set.of(TargetRole.ADMINISTRATIVE_ASSISTANT, TargetRole.SCHOOL_SECRETARY)),
                List.of(new StudyReference("Referência sintética", URI.create("https://example.test/study"), null)),
                new Provenance("Fixture autoral de teste", "fixtures/conteudo.html", "fixtures/gabarito.html", "a".repeat(64), "b".repeat(64)));
    }
}

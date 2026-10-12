package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.application.port.EligibleQuestions;
import java.util.*;

/** Selecionar acervo elegível pela porta de leitura, sem dependência da identidade/editorial CLI. */
public final class BrowseQuestionsUseCase {
    private final EligibleQuestions questions;
    public BrowseQuestionsUseCase(EligibleQuestions questions) { this.questions=Objects.requireNonNull(questions); }
    public EligibleQuestions.Page list(EligibleQuestions.Filter filter) { return questions.list(Objects.requireNonNull(filter)); }
    public EligibleQuestions.Detail detail(UUID id) { return questions.detail(Objects.requireNonNull(id)).orElseThrow(NoSuchElementException::new); }
}

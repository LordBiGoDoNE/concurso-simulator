package br.com.concursosimulator.questionbank.application;

import java.time.Instant;
import java.util.List;

public record ImportReceipt(ImportConfirmation confirmation, Instant committedAt,
        List<ApplyQuestionCandidatesUseCase.Result> results) {
    public ImportReceipt { results=List.copyOf(results); }
}

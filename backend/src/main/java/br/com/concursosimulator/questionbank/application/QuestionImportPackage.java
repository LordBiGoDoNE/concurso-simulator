package br.com.concursosimulator.questionbank.application;

import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import java.util.*;

/** Pacote validado pelo adaptador de entrada; bytes privados nunca fazem parte do resultado HTTP. */
public record QuestionImportPackage(QuestionPurpose purpose, String digest, String selectionDigest,
        List<ApplyQuestionCandidatesUseCase.Candidate> candidates, List<Evidence> evidence, List<List<String>> duplicates) {
    public QuestionImportPackage {
        Objects.requireNonNull(purpose); Objects.requireNonNull(digest); Objects.requireNonNull(selectionDigest);
        candidates=List.copyOf(candidates); evidence=List.copyOf(evidence);
        duplicates=duplicates.stream().map(List::copyOf).toList();
        if(candidates.isEmpty() || candidates.size()>100 || candidates.stream().anyMatch(c->c.origin().purpose()!=purpose))
            throw new IllegalArgumentException("Pacote/coleção inválidos");
    }
    public record Evidence(String path, String mediaType, String sha256, byte[] bytes) {
        public Evidence { bytes=bytes.clone(); }
        @Override public byte[] bytes(){return bytes.clone();}
        @Override public String toString(){return "Evidence[private]";}
    }
    @Override public String toString(){return "QuestionImportPackage[private]";}
}

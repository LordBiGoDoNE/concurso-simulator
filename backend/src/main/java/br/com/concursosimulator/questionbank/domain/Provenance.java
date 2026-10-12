package br.com.concursosimulator.questionbank.domain;

/** Autoria explícita; referências temáticas não são autoria nem validação oficial. */
public record Provenance(String author, String contentPath, String answerPath, String contentSha256, String answerSha256) {
    public Provenance {
        ContentRules.text(author, 500, "Autoria");
        ContentRules.localPath(contentPath);
        ContentRules.localPath(answerPath);
        ContentRules.hash(contentSha256);
        ContentRules.hash(answerSha256);
    }
}

package br.com.concursosimulator.questionbank.application.port;

import br.com.concursosimulator.questionbank.application.QuestionImportPackage;
import br.com.concursosimulator.questionbank.domain.*;
import java.util.*;

/** Entrada estruturada de candidatos; validação de bytes é executada pelo adaptador. */
public interface QuestionImportReader {
    QuestionImportPackage read(Document document,List<String> keys);
    record Document(String format,QuestionPurpose purpose,List<Item> items,List<File> files) {}
    record Item(QuestionOrigin origin,Content content,String fingerprint,UUID expectedLatestRevision) {}
    record File(String path,String mediaType,String sha256,String base64) {}
    record Classification(String disciplineId,String topicId,List<String> roleIds) {}
    record Content(TextContent statement,List<QuestionContent.Alternative> alternatives,UUID correctAlternativeId,
            TextContent resolution,Classification classification,List<StudyReference> references,Provenance provenance) {}
    final class InvalidPackage extends RuntimeException {
        public InvalidPackage(){super("INVALID_PACKAGE");}
    }
}

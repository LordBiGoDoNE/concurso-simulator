package br.com.concursosimulator.questionbank.infrastructure.persistence;

import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.application.port.UnsafeQuestionProjectionException;
import br.com.concursosimulator.shared.infrastructure.QuestionTextSafety;
import java.util.*;
import java.util.regex.Pattern;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Não remove conteúdo incompatível silenciosamente: recusa projeção sem transmitir o payload. */
final class PublicProjectionGuard {
    private static final Pattern CORRECTIVE=Pattern.compile("gabarito|answer|correct|corret[ao]|resolu[cç][aã]o",Pattern.CASE_INSENSITIVE);
    static void plain(String text) { if(text==null || QuestionTextSafety.looksLikeMarkup(text)) throw new UnsafeQuestionProjectionException(); }
    static void check(TextContent statement,List<QuestionContent.Alternative> options,List<StudyReference> references) {
        text(statement); for(var option:options) text(option.content());
        for(var reference:references) {
            plain(reference.name());
            if(CORRECTIVE.matcher(URLDecoder.decode(reference.url().toString(),StandardCharsets.UTF_8)).find() || reference.url().toString().contains("\\")) throw new UnsafeQuestionProjectionException();
        }
    }
    private static void text(TextContent content) {
        plain(content.text());
        for(var resource:content.resources()) {
            plain(resource.description());
            if(!resource.path().startsWith("assets/") || CORRECTIVE.matcher(resource.path()+resource.description()).find()) throw new UnsafeQuestionProjectionException();
        }
    }
}

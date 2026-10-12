package br.com.concursosimulator.shared.infrastructure;

import java.util.regex.Pattern;

/** Detector estrutural, não parser/renderer HTML: comparações matemáticas continuam texto inerte. */
public final class QuestionTextSafety {
    private QuestionTextSafety() {}
    private static final String ATTRIBUTE = "(?:\\s+[a-z_:][a-z0-9_.:-]*\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s\"'=<>`]+)|\\s+(?:disabled|checked|selected|hidden|multiple|readonly|required|autofocus))*";
    private static final Pattern MARKUP = Pattern.compile(
            "<\\s*(?:!|\\?)|<\\s*/\\s*[a-z][a-z0-9:-]*\\s*>"
            + "|<\\s*(?:script|iframe|object|embed|style|link|meta|base)(?:\\s|/?>)"
            + "|<(?:html|body|head|div|span|p|a|img|svg|math|strong|b|em|i|sub|sup|ol|ul|li|br|hr|input|form|button|table|tr|td|th|details|summary|section|article|h[1-6]|code|pre|path|mrow|mi|mn|mo|mfrac|msub|msup)\\b" + ATTRIBUTE + "\\s*/?>"
            + "|<\\s*[a-z][a-z0-9:-]*\\s+[a-z_:][a-z0-9_.:-]*\\s*=", Pattern.CASE_INSENSITIVE);
    public static boolean looksLikeMarkup(String text) { return MARKUP.matcher(text).find(); }
}

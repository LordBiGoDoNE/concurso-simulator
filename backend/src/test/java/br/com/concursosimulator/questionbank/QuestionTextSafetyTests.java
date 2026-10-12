package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.shared.infrastructure.QuestionTextSafety;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class QuestionTextSafetyTests {
    @Test void mathematicalComparisonsAndLiteralIndicesAreNotHtml() {
        for(var text:List.of("x < y e y > z","x < p e p > z","x<y>z","2 < 3","_abc","(x)_(1) + (y)_(2)","x^(2)"))
            assertThat(QuestionTextSafety.looksLikeMarkup(text)).as(text).isFalse();
    }
    @Test void activeTagsAttributesClosingsAndDeclarationsRemainBlocked() {
        for(var text:List.of("<script>bad()</script>","<SCRIPT anything>","<span data-correct='true'>texto</span>","<p>texto</p>","<img src=x onerror=bad()>","<svg onload='bad()'>","<unknown onclick='bad()'>","<!-- comment -->","<!DOCTYPE html>","</custom>"))
            assertThat(QuestionTextSafety.looksLikeMarkup(text)).as(text).isTrue();
    }
}

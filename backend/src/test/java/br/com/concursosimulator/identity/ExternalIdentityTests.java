package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.domain.ExternalIdentity;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalIdentityTests {
    @Test
    void rejectsMissingBlankAndOversizedValues() {
        for (String invalid : new String[]{null, "", "   ", "x".repeat(256)}) {
            assertThatThrownBy(() -> new ExternalIdentity("google", "https://accounts.google.com", invalid))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Subject inválido");
            assertThatThrownBy(() -> new ExternalIdentity("google", invalid, "subject"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Issuer inválido");
        }
        for (String invalid : new String[]{null, "", "  ", "x".repeat(33)}) {
            assertThatThrownBy(() -> new ExternalIdentity(invalid, "issuer", "subject"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Provider inválido");
        }
    }

    @Test
    void preservesOpaqueValuesAndDoesNotPrintPersonalData() {
        var identity = new ExternalIdentity("google", "https://accounts.google.com", " Case-Sensitive-Subject ");
        assertThat(identity.subject()).isEqualTo(" Case-Sensitive-Subject ");
        assertThat(identity).isNotEqualTo(new ExternalIdentity("google", identity.issuer(), "case-sensitive-subject"));
        assertThat(identity.toString()).isEqualTo("ExternalIdentity[redacted]");
    }
}

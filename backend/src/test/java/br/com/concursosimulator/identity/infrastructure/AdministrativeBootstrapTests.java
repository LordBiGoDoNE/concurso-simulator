package br.com.concursosimulator.identity.infrastructure;

import br.com.concursosimulator.identity.application.AdministrativeAccessUseCases;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.*;
import org.mockito.ArgumentCaptor;
import java.io.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class AdministrativeBootstrapTests {
    @Test void generatedPasswordIsStrongAndEmittedOnlyByCommittedWinner(CapturedOutput output)throws Exception {
        var access=mock(AdministrativeAccessUseCases.class);
        when(access.bootstrap(eq("admin"),anyString(),eq(true))).thenReturn(true,false);
        var runner=new AdministrativeConfiguration().administrativeBootstrap(access,"admin","");
        runner.run(new DefaultApplicationArguments());runner.run(new DefaultApplicationArguments());
        var secret=ArgumentCaptor.forClass(String.class);verify(access,times(2)).bootstrap(eq("admin"),secret.capture(),eq(true));
        assertThat(secret.getAllValues()).allSatisfy(p->assertThat(p).matches("[A-Za-z0-9_-]{43}"));
        assertThat(secret.getAllValues().get(0)).isNotEqualTo(secret.getAllValues().get(1));
        assertThat(output.getOut()).containsOnlyOnce(secret.getAllValues().get(0)).doesNotContain(secret.getAllValues().get(1));
    }
    @Test void suppliedPasswordAndFailedCommitNeverAppearOnConsole(CapturedOutput output)throws Exception {
        var access=mock(AdministrativeAccessUseCases.class);String secret="synthetic-configured-password";
        when(access.bootstrap("admin",secret,false)).thenReturn(true);
        new AdministrativeConfiguration().administrativeBootstrap(access,"admin",secret).run(new DefaultApplicationArguments());
        when(access.bootstrap(eq("admin"),anyString(),eq(true))).thenThrow(new IllegalStateException("synthetic commit failure"));
        assertThatThrownBy(()->new AdministrativeConfiguration().administrativeBootstrap(access,"admin","").run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(output.getAll()).doesNotContain(secret,"initial temporary password");
    }
    @Test void adaptiveEncodingHasRandomSaltAndRejectsOtherPasswords(){
        var passwords=new AdministrativeConfiguration().administrativePasswords();String secret="synthetic-password-for-encoding";
        String first=passwords.encode(secret),second=passwords.encode(secret);
        assertThat(first).isNotEqualTo(second).startsWith("{pbkdf2-sha256-600k}");
        assertThat(passwords.matches(secret,first)).isTrue();assertThat(passwords.matches("incorrect-password",first)).isFalse();
        assertThat(passwords.matches(secret,"unversioned")).isFalse();
    }
    @Test void recoveryRequiresExplicitLocalTargetAndConfirmationBeforeOpeningDatabase(){
        var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
        var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/fixture","DB_USERNAME","fixture","DB_PASSWORD","SENTINEL_PRIVATE");
        assertThat(AdministrativeRecovery.launch(new String[]{"--recover-admin"},env,new BufferedReader(new StringReader("cancel\n")),new PrintStream(out),new PrintStream(err))).isEqualTo(2);
        assertThat(out.toString()).contains("Cancelled").doesNotContain("SENTINEL_PRIVATE");
        assertThatThrownBy(()->AdministrativeRecovery.destination("jdbc:postgresql://remote.example/private")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->AdministrativeRecovery.destination("jdbc:postgresql://user:secret@localhost/private")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->AdministrativeRecovery.destination("jdbc:postgresql://localhost/private?password=secret")).isInstanceOf(IllegalArgumentException.class);
    }
}

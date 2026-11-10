package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.domain.AdministrativeAccount;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** Domain tests have no Spring, database or encoder dependency. */
class AdministrativeAccountTests {
    AdministrativeAccount account(){return new AdministrativeAccount(UUID.randomUUID(),"admin","private-hash",true,true,1,0,null,null);}
    @Test void temporaryCredentialOnlyAllowsSetupAndVersionIsRequired(){
        var a=account();assertThat(a.permits(a.owner(),1,true)).isTrue();assertThat(a.permits(a.owner(),1,false)).isFalse();
        assertThat(a.permits(UUID.randomUUID(),1,true)).isFalse();assertThat(a.permits(a.owner(),2,true)).isFalse();
    }
    @Test void passwordAndLinkTransitionsAdvanceEpochWithoutCopyingHistory(){
        var a=account();var changed=a.password("new-hash",false);assertThat(changed.version()).isEqualTo(2);assertThat(changed.owner()).isEqualTo(a.owner());
        assertThat(changed.permits(a.owner(),2,false)).isTrue();var target=UUID.randomUUID();var linked=changed.linked(target);
        assertThat(linked.owner()).isEqualTo(target);assertThat(linked.passwordHash()).isEqualTo(changed.passwordHash());assertThat(linked.version()).isEqualTo(3);
        assertThat(linked.toString()).doesNotContain("private-hash","new-hash");
    }
    @Test void fiveFailuresBlockForFifteenMinutesAndWindowCanReset(){
        var a=account();var now=Instant.parse("2026-11-09T10:00:00Z");
        for(int i=0;i<5;i++)a=a.failed(now.plusSeconds(i));
        assertThat(a.blocked(now.plusSeconds(903))).isTrue();assertThat(a.blocked(now.plusSeconds(904))).isFalse();
        assertThat(a.failed(now.plusSeconds(904)).failures()).isEqualTo(1);assertThat(a.authenticated().failures()).isZero();
    }
    @Test void disabledAccountCannotGainAccessAndPasswordPolicyNeverTrims(){
        var a=account();var disabled=new AdministrativeAccount(a.owner(),a.username(),a.passwordHash(),false,false,1,0,null,null);
        assertThat(disabled.permits(a.owner(),1,true)).isFalse();
        assertThatThrownBy(()->AdministrativeAccount.validatePassword("x".repeat(14))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->AdministrativeAccount.validatePassword("x".repeat(129))).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(()->AdministrativeAccount.validatePassword(" "+"x".repeat(14))).doesNotThrowAnyException();
        assertThatCode(()->AdministrativeAccount.validatePassword("\uD83D\uDE00".repeat(128))).doesNotThrowAnyException();
        assertThatThrownBy(()->AdministrativeAccount.validatePassword("\uD83D\uDE00".repeat(129))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->AdministrativeAccount.validatePassword("valid-password-"+'\uD800')).isInstanceOf(IllegalArgumentException.class);
    }
}

package br.com.concursosimulator.identity;

import br.com.concursosimulator.identity.application.ResolveExternalIdentityUseCase;
import br.com.concursosimulator.identity.application.port.IdentityAlreadyLinkedException;
import br.com.concursosimulator.identity.application.port.IdentityRepository;
import br.com.concursosimulator.identity.domain.ExternalIdentity;
import br.com.concursosimulator.shared.application.port.UnitOfWork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ResolveExternalIdentityUseCaseTests {
    final IdentityRepository repository = mock(IdentityRepository.class);
    final ExternalIdentity identity = new ExternalIdentity("google", "https://accounts.google.com", "opaque-subject");
    final List<String> events = new ArrayList<>();
    final UnitOfWork transactions = new UnitOfWork() {
        @Override public <T> T independently(Supplier<T> operation) {
            events.add("begin");
            try { T result = operation.get(); events.add("commit"); return result; }
            catch (RuntimeException exception) { events.add("rollback"); throw exception; }
        }
    };
    final ResolveExternalIdentityUseCase useCase = new ResolveExternalIdentityUseCase(repository, transactions);

    @Test
    void existingIdentityDoesNotCreateUser() {
        UUID id = UUID.randomUUID();
        when(repository.findUserId(identity)).thenReturn(Optional.of(id));
        assertThat(useCase.execute(identity)).isEqualTo(id);
        verify(repository, never()).createUserWithIdentity(any(), any());
        assertThat(events).containsExactly("begin", "commit");
    }

    @Test
    void createsInternalUuidUsingOnlyTheRepositoryPort() {
        when(repository.findUserId(identity)).thenReturn(Optional.empty());
        UUID id = useCase.execute(identity);
        var saved = ArgumentCaptor.forClass(UUID.class);
        verify(repository).createUserWithIdentity(saved.capture(), eq(identity));
        assertThat(saved.getValue()).isEqualTo(id);
        assertThat(events).containsExactly("begin", "commit");
    }

    @Test
    void resolvesConflictOnlyAfterRollbackInAnotherUnit() {
        UUID winner = UUID.randomUUID();
        var conflict = new IdentityAlreadyLinkedException(null);
        when(repository.findUserId(identity)).thenReturn(Optional.empty()).thenAnswer(call -> {
            assertThat(events).containsExactly("begin", "rollback", "begin");
            return Optional.of(winner);
        });
        doThrow(conflict).when(repository).createUserWithIdentity(any(), eq(identity));
        assertThat(useCase.execute(identity)).isEqualTo(winner);
        assertThat(events).containsExactly("begin", "rollback", "begin", "commit");
    }

    @Test
    void missingWinnerRethrowsConflictInsteadOfInventingAnAccount() {
        var conflict = new IdentityAlreadyLinkedException(null);
        when(repository.findUserId(identity)).thenReturn(Optional.empty());
        doThrow(conflict).when(repository).createUserWithIdentity(any(), any());
        assertThatThrownBy(() -> useCase.execute(identity)).isSameAs(conflict);
        assertThat(events).containsExactly("begin", "rollback", "begin", "rollback");
    }

    @Test
    void unrelatedFailuresAreNotRetriedAsIdentityConflicts() {
        var failure = new IllegalStateException("Storage unavailable");
        when(repository.findUserId(identity)).thenThrow(failure);
        assertThatThrownBy(() -> useCase.execute(identity)).isSameAs(failure);
        verify(repository, times(1)).findUserId(identity);
        assertThat(events).containsExactly("begin", "rollback");
    }
}

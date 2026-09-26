package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class UserAuthCacheTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAuthCache cache = new UserAuthCache(userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    private final User user = new User("lia@exemplo.com", "Lia", UserRole.CANDIDATE);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void givenCachedSnapshot_whenReadingAgainWithinTtl_thenRepositoryIsHitOnce() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        cache.snapshotOf(user.getId());
        user.promoteToAdmin();

        Optional<AuthSnapshot> snapshot = cache.snapshotOf(user.getId());

        assertThat(snapshot).contains(new AuthSnapshot(UserRole.CANDIDATE, false));
        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    void givenNoActiveTransaction_whenEvicting_thenNextReadReflectsTheChangeImmediately() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        cache.snapshotOf(user.getId());
        user.acceptTerms("2026-09-01", NOW);

        cache.evictAfterCommit(user.getId());

        assertThat(cache.snapshotOf(user.getId())).contains(new AuthSnapshot(UserRole.CANDIDATE, true));
    }

    @Test
    void givenActiveTransaction_whenEvicting_thenSnapshotIsKeptUntilCommit() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        cache.snapshotOf(user.getId());
        user.promoteToAdmin();
        TransactionSynchronizationManager.initSynchronization();

        cache.evictAfterCommit(user.getId());

        assertThat(cache.snapshotOf(user.getId())).contains(new AuthSnapshot(UserRole.CANDIDATE, false));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(cache.snapshotOf(user.getId())).contains(new AuthSnapshot(UserRole.ADMIN, false));
    }
}

package br.com.certamecards.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.UserAuthCache;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TermsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    private final AccountService accountService = mock(AccountService.class);
    private final UserAuthCache userAuthCache = mock(UserAuthCache.class);
    private final TermsService termsService =
            new TermsService(accountService, userAuthCache, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void givenExistingUser_whenAccepting_thenRecordsVersionAndEvictsAuthSnapshot() {
        User user = new User("rui@exemplo.com", "Rui", UserRole.CANDIDATE);
        when(accountService.getProfile(user.getId())).thenReturn(user);

        termsService.accept(user.getId(), "2026-09-01");

        assertThat(user.getTerms().getVersion()).isEqualTo("2026-09-01");
        assertThat(user.getTerms().getAcceptedAt()).isEqualTo(NOW);
        verify(userAuthCache).evictAfterCommit(user.getId());
    }

    @Test
    void givenUnknownUser_whenAccepting_thenPropagatesNotFoundWithoutEvicting() {
        UUID id = UUID.randomUUID();
        when(accountService.getProfile(id)).thenThrow(ApiException.of(ErrorCode.NOT_FOUND));

        assertThatThrownBy(() -> termsService.accept(id, "2026-09-01")).isInstanceOf(ApiException.class);
        verify(userAuthCache, never()).evictAfterCommit(any());
    }
}

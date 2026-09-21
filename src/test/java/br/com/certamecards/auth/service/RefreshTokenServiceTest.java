package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshCookieProperties cookieProperties =
            new RefreshCookieProperties("__Host-refresh", Duration.ofDays(30), true);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RefreshTokenService service =
            new RefreshTokenService(repository, cookieProperties, clock, meterRegistry);

    @Test
    void givenValidToken_whenRotating_thenRevokesOldAndSavesNewLinkedByFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken current =
                new RefreshToken(UUID.randomUUID(), USER_ID, familyId, "hash", NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(current));

        RotationResult result = service.rotate("raw-token", "agent");

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.newToken().entity().getFamilyId()).isEqualTo(familyId);
        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(current.getReplacedBy()).isEqualTo(result.newToken().entity().getId());
        verify(repository, times(2)).save(any());
    }

    @Test
    void givenAlreadyRevokedToken_whenRotatingAgain_thenDetectsReuseAndRevokesFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken revoked =
                new RefreshToken(UUID.randomUUID(), USER_ID, familyId, "hash", NOW.plus(Duration.ofDays(1)));
        revoked.revoke(NOW.minusSeconds(60));
        RefreshToken sibling =
                new RefreshToken(UUID.randomUUID(), USER_ID, familyId, "hash2", NOW.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(revoked));
        when(repository.findByFamilyIdAndRevokedAtIsNull(familyId)).thenReturn(List.of(sibling));

        assertThatThrownBy(() -> service.rotate("raw-token", "agent"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

        assertThat(sibling.getRevokedAt()).isEqualTo(NOW);
        assertThat(meterRegistry.counter("auth.refresh.reuse_detected").count()).isEqualTo(1.0);
    }

    @Test
    void givenExpiredToken_whenRotating_thenThrowsUnauthenticated() {
        RefreshToken expired =
                new RefreshToken(UUID.randomUUID(), USER_ID, UUID.randomUUID(), "hash", NOW.minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("raw-token", "agent"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void givenUnknownToken_whenRotating_thenThrowsUnauthenticated() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("raw-token", "agent"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void givenUserId_whenIssuingNewFamily_thenSavesTokenWithFreshFamilyId() {
        IssuedRefreshToken issued = service.issueNewFamily(USER_ID, "agent");

        assertThat(issued.entity().getUserId()).isEqualTo(USER_ID);
        assertThat(issued.entity().getExpiresAt()).isEqualTo(NOW.plus(cookieProperties.ttl()));
        verify(repository).save(issued.entity());
    }

    @Test
    void givenUserId_whenRevokingAllForUser_thenRevokesEachActiveToken() {
        RefreshToken tokenOne =
                new RefreshToken(UUID.randomUUID(), USER_ID, UUID.randomUUID(), "hash1", NOW.plus(Duration.ofDays(1)));
        RefreshToken tokenTwo =
                new RefreshToken(UUID.randomUUID(), USER_ID, UUID.randomUUID(), "hash2", NOW.plus(Duration.ofDays(1)));
        when(repository.findByUserIdAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(tokenOne, tokenTwo));

        service.revokeAllForUser(USER_ID);

        assertThat(tokenOne.getRevokedAt()).isEqualTo(NOW);
        assertThat(tokenTwo.getRevokedAt()).isEqualTo(NOW);
    }
}

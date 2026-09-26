package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OneTimeTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OneTimeTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Duration CONFIRM_EMAIL_TTL = Duration.ofMinutes(60);

    private final OneTimeTokenRepository repository = mock(OneTimeTokenRepository.class);
    private final OneTimeTokenProperties properties = new OneTimeTokenProperties(
            CONFIRM_EMAIL_TTL, Duration.ofMinutes(60), Duration.ofMinutes(60), Duration.ofMinutes(15));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final OneTimeTokenService service = new OneTimeTokenService(repository, properties, clock);

    @Test
    @DisplayName("TU-26 — token emitido expira conforme o TTL da finalidade")
    void givenTokenIssued_whenIssuing_thenExpiresAtMatchesPurposeTtl() {
        IssuedToken issued = service.issue(USER_ID, OneTimeTokenPurpose.CONFIRM_EMAIL);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(CONFIRM_EMAIL_TTL));
    }

    @Test
    @DisplayName("TU-26 — token com 61 min lança token_expired")
    void givenTokenAgedSixtyOneMinutes_whenConsuming_thenThrowsTokenExpired() {
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(),
                USER_ID,
                OneTimeTokenPurpose.CONFIRM_EMAIL,
                "hash",
                NOW.minus(Duration.ofMinutes(1)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("raw-token", OneTimeTokenPurpose.CONFIRM_EMAIL))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("TU-26 — segundo uso do mesmo token lança token_used")
    void givenTokenAlreadyUsed_whenConsumingSecondTime_thenThrowsTokenUsed() {
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(),
                USER_ID,
                OneTimeTokenPurpose.CONFIRM_EMAIL,
                "hash",
                NOW.plus(Duration.ofMinutes(30)));
        token.markUsed(NOW.minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("raw-token", OneTimeTokenPurpose.CONFIRM_EMAIL))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_USED));
    }

    @Test
    @DisplayName("TU-26 — token válido e não usado é consumido e marcado como usado")
    void givenValidUnusedToken_whenConsuming_thenMarksUsedAndReturnsToken() {
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(),
                USER_ID,
                OneTimeTokenPurpose.CONFIRM_EMAIL,
                "hash",
                NOW.plus(Duration.ofMinutes(30)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        OneTimeToken consumed = service.consume("raw-token", OneTimeTokenPurpose.CONFIRM_EMAIL);

        assertThat(consumed.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("TU-26 — finalidade diferente da esperada lança token_expired")
    void givenTokenWithDifferentPurpose_whenConsuming_thenThrowsTokenExpired() {
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(),
                USER_ID,
                OneTimeTokenPurpose.RESET_PASSWORD,
                "hash",
                NOW.plus(Duration.ofMinutes(30)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("raw-token", OneTimeTokenPurpose.CONFIRM_EMAIL))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }
}

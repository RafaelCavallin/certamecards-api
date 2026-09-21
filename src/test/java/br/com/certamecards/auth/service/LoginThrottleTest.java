package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.LoginAttempt;
import br.com.certamecards.auth.persistence.LoginAttemptRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class LoginThrottleTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    private final LoginAttemptRepository repository = mock(LoginAttemptRepository.class);
    private final LoginThrottleProperties properties = new LoginThrottleProperties(5, Duration.ofMinutes(15));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final LoginThrottle throttle = new LoginThrottle(repository, properties, clock);

    @Test
    void givenFewerThanFiveRecentFailures_whenEnsuringAllowed_thenDoesNotThrow() {
        List<LoginAttempt> failures = List.of(failureAt(NOW.minusSeconds(60)));
        when(repository.findByEmailAndSuccessFalseOrderByAttemptedAtDesc(EMAIL, PageRequest.of(0, 5)))
                .thenReturn(failures);
        throttle.ensureAllowed(EMAIL);
    }

    @Test
    void givenFiveFailuresWithinWindow_whenEnsuringAllowed_thenThrowsLoginLocked() {
        List<LoginAttempt> failures = List.of(
                failureAt(NOW.minusSeconds(10)),
                failureAt(NOW.minusSeconds(20)),
                failureAt(NOW.minusSeconds(30)),
                failureAt(NOW.minusSeconds(40)),
                failureAt(NOW.minus(Duration.ofMinutes(10))));
        when(repository.findByEmailAndSuccessFalseOrderByAttemptedAtDesc(EMAIL, PageRequest.of(0, 5)))
                .thenReturn(failures);
        assertThatThrownBy(() -> throttle.ensureAllowed(EMAIL))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.LOGIN_LOCKED));
    }

    @Test
    void givenFiveFailuresOutsideWindow_whenEnsuringAllowed_thenDoesNotThrow() {
        List<LoginAttempt> failures = List.of(
                failureAt(NOW.minus(Duration.ofMinutes(16))),
                failureAt(NOW.minus(Duration.ofMinutes(17))),
                failureAt(NOW.minus(Duration.ofMinutes(18))),
                failureAt(NOW.minus(Duration.ofMinutes(19))),
                failureAt(NOW.minus(Duration.ofMinutes(20))));
        when(repository.findByEmailAndSuccessFalseOrderByAttemptedAtDesc(EMAIL, PageRequest.of(0, 5)))
                .thenReturn(failures);
        throttle.ensureAllowed(EMAIL);
    }

    private LoginAttempt failureAt(Instant attemptedAt) {
        LoginAttempt attempt = mock(LoginAttempt.class);
        when(attempt.getAttemptedAt()).thenReturn(attemptedAt);
        return attempt;
    }
}

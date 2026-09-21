package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginAttemptTest {

    @Test
    void givenEmailIpAndSuccess_whenConstructing_thenGettersReturnValues() {
        LoginAttempt attempt = new LoginAttempt("ana@exemplo.com", "203.0.113.10", false);
        assertThat(attempt.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(attempt.getIp()).isEqualTo("203.0.113.10");
        assertThat(attempt.isSuccess()).isFalse();
        assertThat(attempt.getId()).isNotNull();
        assertThat(attempt.getAttemptedAt()).isNull();
    }

    @Test
    void givenAttemptedAt_whenConstructing_thenGetterReturnsProvidedInstant() {
        Instant attemptedAt = Instant.parse("2026-09-17T12:00:00Z");
        LoginAttempt attempt = new LoginAttempt("ana@exemplo.com", "203.0.113.10", true, attemptedAt);
        assertThat(attempt.getAttemptedAt()).isEqualTo(attemptedAt);
    }
}

package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, RefreshTokenReuseIT.FixedClockConfig.class})
class RefreshTokenReuseIT {

    private static final String RAW_TOKEN = "reused-refresh-token";
    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");

    @Autowired
    private RefreshTokenService service;

    @Autowired
    private RefreshTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("TU-25 — reuso persiste a revogação da família apesar da resposta 401")
    void givenReusedRefreshToken_whenRotating_thenFamilyRevocationCommits() {
        User user = userRepository.save(new User("refresh-reuse@exemplo.com", "Reuse", UserRole.CANDIDATE));
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = NOW.plusSeconds(86_400);
        RefreshToken reused = token(RAW_TOKEN, user.getId(), familyId, expiresAt);
        reused.revoke(NOW.minusSeconds(1));
        RefreshToken sibling = token("active-sibling", user.getId(), familyId, expiresAt);
        tokenRepository.save(reused);
        tokenRepository.save(sibling);
        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "agent")).isInstanceOf(ApiException.class);
        RefreshToken persisted = tokenRepository.findById(sibling.getId()).orElseThrow();
        assertThat(persisted.getRevokedAt()).isNotNull();
    }

    private RefreshToken token(String raw, UUID userId, UUID familyId, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, familyId, TokenHasher.hash(raw), expiresAt, NOW);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}

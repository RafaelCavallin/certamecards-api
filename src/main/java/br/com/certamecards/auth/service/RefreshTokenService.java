package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final RefreshCookieProperties cookieProperties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            RefreshCookieProperties cookieProperties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamily(UUID userId, String userAgent) {
        return saveNewToken(userId, UUID.randomUUID(), userAgent);
    }

    @Transactional
    public RotationResult rotate(String rawToken, String userAgent) {
        RefreshToken current = findValidOrReject(rawToken);
        IssuedRefreshToken next = saveNewToken(current.getUserId(), current.getFamilyId(), userAgent);
        current.revoke(clock.instant());
        current.replaceBy(next.entity().getId());
        repository.save(current);
        return new RotationResult(current.getUserId(), next);
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        repository.findByTokenHash(TokenHasher.hash(rawToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            repository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(clock.instant()));
    }

    public RefreshCookieProperties cookieProperties() {
        return cookieProperties;
    }

    private RefreshToken findValidOrReject(String rawToken) {
        RefreshToken token = repository
                .findByTokenHash(TokenHasher.hash(rawToken))
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHENTICATED));
        if (token.getRevokedAt() != null) {
            meterRegistry.counter("auth.refresh.reuse_detected").increment();
            revokeFamily(token.getFamilyId());
            throw ApiException.of(ErrorCode.UNAUTHENTICATED);
        }
        if (token.getExpiresAt().isBefore(clock.instant())) {
            throw ApiException.of(ErrorCode.UNAUTHENTICATED);
        }
        return token;
    }

    private void revokeFamily(UUID familyId) {
        repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(token -> token.revoke(clock.instant()));
    }

    private IssuedRefreshToken saveNewToken(UUID userId, UUID familyId, String userAgent) {
        String rawToken = TokenHasher.generateRawToken();
        Instant now = clock.instant();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(), userId, familyId, TokenHasher.hash(rawToken), now.plus(cookieProperties.ttl()), now);
        token.assignUserAgent(userAgent);
        repository.save(token);
        return new IssuedRefreshToken(rawToken, token);
    }
}

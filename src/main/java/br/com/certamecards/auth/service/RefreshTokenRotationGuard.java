package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.auth.persistence.RefreshFamilyLock;
import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRotationGuard {

    private final RefreshTokenRepository repository;
    private final RefreshFamilyLock familyLock;
    private final RefreshTokenReuseDetector reuseDetector;

    public RefreshTokenRotationGuard(
            RefreshTokenRepository repository, RefreshFamilyLock familyLock, RefreshTokenReuseDetector reuseDetector) {
        this.repository = repository;
        this.familyLock = familyLock;
        this.reuseDetector = reuseDetector;
    }

    public RefreshToken validToken(String rawToken, Instant now) {
        String tokenHash = TokenHasher.hash(rawToken);
        UUID familyId = repository
                .findFamilyIdByTokenHash(tokenHash)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHENTICATED));
        familyLock.lock(familyId);
        RefreshToken token = repository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHENTICATED));
        if (token.getRevokedAt() != null) {
            reuseDetector.reject(token, now);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw ApiException.of(ErrorCode.UNAUTHENTICATED);
        }
        return token;
    }
}

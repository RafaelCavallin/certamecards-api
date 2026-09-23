package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenIssuer issuer;
    private final RefreshTokenRotationGuard rotationGuard;
    private final RefreshTokenRevocation revocation;

    public RefreshTokenService(
            RefreshTokenIssuer issuer, RefreshTokenRotationGuard rotationGuard, RefreshTokenRevocation revocation) {
        this.issuer = issuer;
        this.rotationGuard = rotationGuard;
        this.revocation = revocation;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamily(UUID userId, String userAgent) {
        return issuer.issueNewFamily(userId, userAgent);
    }

    @Transactional
    public RotationResult rotate(String rawToken, String userAgent) {
        RefreshToken current = rotationGuard.validToken(rawToken, issuer.now());
        return issuer.rotate(current, userAgent);
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        revocation.revokeByRawToken(rawToken);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        revocation.revokeAllForUser(userId);
    }

    public RefreshCookieProperties cookieProperties() {
        return issuer.cookieProperties();
    }
}

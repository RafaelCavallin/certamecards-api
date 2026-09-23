package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.RefreshToken;
import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenIssuer {

    private final RefreshTokenRepository repository;
    private final RefreshCookieProperties cookieProperties;
    private final Clock clock;

    public RefreshTokenIssuer(
            RefreshTokenRepository repository, RefreshCookieProperties cookieProperties, Clock clock) {
        this.repository = repository;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
    }

    public IssuedRefreshToken issueNewFamily(UUID userId, String userAgent) {
        return saveNewToken(userId, UUID.randomUUID(), userAgent);
    }

    public RotationResult rotate(RefreshToken current, String userAgent) {
        IssuedRefreshToken next = saveNewToken(current.getUserId(), current.getFamilyId(), userAgent);
        current.revoke(now());
        current.replaceBy(next.entity().getId());
        repository.save(current);
        return new RotationResult(current.getUserId(), next);
    }

    public RefreshCookieProperties cookieProperties() {
        return cookieProperties;
    }

    public Instant now() {
        return clock.instant();
    }

    private IssuedRefreshToken saveNewToken(UUID userId, UUID familyId, String userAgent) {
        String rawToken = TokenHasher.generateRawToken();
        Instant issuedAt = now();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                userId,
                familyId,
                TokenHasher.hash(rawToken),
                issuedAt.plus(cookieProperties.ttl()),
                issuedAt);
        token.assignUserAgent(userAgent);
        repository.save(token);
        return new IssuedRefreshToken(rawToken, token);
    }
}

package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OneTimeTokenRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OneTimeTokenService {

    private final OneTimeTokenRepository repository;
    private final OneTimeTokenProperties properties;
    private final Clock clock;

    public OneTimeTokenService(OneTimeTokenRepository repository, OneTimeTokenProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedToken issue(UUID userId, OneTimeTokenPurpose purpose) {
        return issueWithPayload(userId, purpose, null);
    }

    @Transactional
    public IssuedToken issueWithPayload(UUID userId, OneTimeTokenPurpose purpose, String payload) {
        String rawToken = TokenHasher.generateRawToken();
        Instant expiresAt = clock.instant().plus(ttlFor(purpose));
        OneTimeToken token =
                new OneTimeToken(UUID.randomUUID(), userId, purpose, TokenHasher.hash(rawToken), expiresAt);
        token.assignPayload(payload);
        repository.save(token);
        return new IssuedToken(rawToken, expiresAt);
    }

    @Transactional
    public OneTimeToken consume(String rawToken, OneTimeTokenPurpose purpose) {
        OneTimeToken token = repository
                .findByTokenHash(TokenHasher.hash(rawToken))
                .filter(candidate -> candidate.getPurpose() == purpose)
                .orElseThrow(() -> ApiException.of(ErrorCode.TOKEN_EXPIRED));
        if (token.getUsedAt() != null) {
            throw ApiException.of(ErrorCode.TOKEN_USED);
        }
        if (token.getExpiresAt().isBefore(clock.instant())) {
            throw ApiException.of(ErrorCode.TOKEN_EXPIRED);
        }
        token.markUsed(clock.instant());
        return token;
    }

    private Duration ttlFor(OneTimeTokenPurpose purpose) {
        return switch (purpose) {
            case CONFIRM_EMAIL -> properties.confirmEmailTtl();
            case RESET_PASSWORD -> properties.resetPasswordTtl();
            case LINK_GOOGLE -> properties.linkGoogleTtl();
            case REAUTH -> properties.reauthTtl();
        };
    }
}

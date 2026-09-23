package br.com.certamecards.auth.service;

import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRevocation {

    private final RefreshTokenRepository repository;
    private final Clock clock;

    public RefreshTokenRevocation(RefreshTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void revokeByRawToken(String rawToken) {
        repository.findByTokenHash(TokenHasher.hash(rawToken)).ifPresent(token -> {
            token.revoke(clock.instant());
            repository.save(token);
        });
    }

    public void revokeAllForUser(UUID userId) {
        repository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(clock.instant()));
    }
}

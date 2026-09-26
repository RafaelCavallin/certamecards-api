package br.com.certamecards.testsupport.service;

import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSupportSessionRevoker {

    private final RefreshTokenRepository repository;
    private final Clock clock;

    public TestSupportSessionRevoker(RefreshTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(clock.instant()));
    }
}

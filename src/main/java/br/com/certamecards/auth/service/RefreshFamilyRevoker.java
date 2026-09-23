package br.com.certamecards.auth.service;

import br.com.certamecards.auth.persistence.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshFamilyRevoker {

    private final RefreshTokenRepository repository;

    public RefreshFamilyRevoker(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(UUID familyId, Instant revokedAt) {
        repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(token -> token.revoke(revokedAt));
    }
}

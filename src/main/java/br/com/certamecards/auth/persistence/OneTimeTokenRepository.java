package br.com.certamecards.auth.persistence;

import br.com.certamecards.auth.domain.OneTimeToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHash(String tokenHash);
}

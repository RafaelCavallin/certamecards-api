package br.com.certamecards.auth.persistence;

import br.com.certamecards.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select token.familyId from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findFamilyIdByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}

package br.com.certamecards.auth.persistence;

import br.com.certamecards.auth.domain.OauthIdentity;
import br.com.certamecards.auth.domain.OauthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, UUID> {

    Optional<OauthIdentity> findByProviderAndSubject(OauthProvider provider, String subject);
}

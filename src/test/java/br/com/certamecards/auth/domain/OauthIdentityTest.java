package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OauthIdentityTest {

    @Test
    void givenUserIdProviderSubjectAndEmail_whenConstructing_thenGettersReturnValues() {
        UUID userId = UUID.randomUUID();
        OauthIdentity identity = new OauthIdentity(userId, OauthProvider.GOOGLE, "sub-1", "ana@exemplo.com");
        assertThat(identity.getUserId()).isEqualTo(userId);
        assertThat(identity.getProvider()).isEqualTo(OauthProvider.GOOGLE);
        assertThat(identity.getSubject()).isEqualTo("sub-1");
        assertThat(identity.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(identity.getId()).isNotNull();
        assertThat(identity.getCreatedAt()).isNull();
    }

    @Test
    void givenCreatedAt_whenConstructing_thenGetterReturnsProvidedInstant() {
        Instant createdAt = Instant.parse("2026-09-17T12:00:00Z");
        OauthIdentity identity =
                new OauthIdentity(UUID.randomUUID(), OauthProvider.GOOGLE, "sub-1", "ana@exemplo.com", createdAt);
        assertThat(identity.getCreatedAt()).isEqualTo(createdAt);
    }
}

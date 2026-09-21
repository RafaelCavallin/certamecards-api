package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.OauthIdentity;
import br.com.certamecards.auth.domain.OauthProvider;
import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OauthIdentityRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class GoogleLinkServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String SUBJECT = "google-subject-123";
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    private final OauthIdentityRepository oauthIdentityRepository = mock(OauthIdentityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final java.time.Clock clock = java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    private final GoogleLinkService service =
            new GoogleLinkService(oauthIdentityRepository, userRepository, oneTimeTokenService, passwordEncoder, clock);

    @Test
    void givenLinkedSubject_whenHandlingReauth_thenIssuesReauthToken() {
        OauthIdentity identity = new OauthIdentity(UUID.randomUUID(), OauthProvider.GOOGLE, SUBJECT, EMAIL);
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.of(identity));
        when(oneTimeTokenService.issue(identity.getUserId(), OneTimeTokenPurpose.REAUTH))
                .thenReturn(new IssuedToken("raw-reauth", NOW.plusSeconds(900)));

        String rawToken = service.handleReauth(SUBJECT);

        assertThat(rawToken).isEqualTo("raw-reauth");
    }

    @Test
    void givenUnlinkedSubject_whenHandlingReauth_thenThrowsNotFound() {
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleReauth(SUBJECT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenCorrectPassword_whenLinkingWithPassword_thenSavesIdentityAndReturnsUser() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed");
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(), user.getId(), OneTimeTokenPurpose.LINK_GOOGLE, "hash", NOW.plusSeconds(60));
        token.assignPayload(GoogleTokenPayload.quote(SUBJECT));
        when(oneTimeTokenService.consume("raw-link-token", OneTimeTokenPurpose.LINK_GOOGLE))
                .thenReturn(token);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha", "hashed")).thenReturn(true);

        User result = service.linkWithPassword("raw-link-token", "senha");

        assertThat(result.getId()).isEqualTo(user.getId());
        verify(oauthIdentityRepository).save(org.mockito.ArgumentMatchers.argThat(identity -> identity.getSubject()
                .equals(SUBJECT)));
    }

    @Test
    void givenWrongPassword_whenLinkingWithPassword_thenThrowsInvalidCredentials() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed");
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(), user.getId(), OneTimeTokenPurpose.LINK_GOOGLE, "hash", NOW.plusSeconds(60));
        token.assignPayload(GoogleTokenPayload.quote(SUBJECT));
        when(oneTimeTokenService.consume("raw-link-token", OneTimeTokenPurpose.LINK_GOOGLE))
                .thenReturn(token);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-errada", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.linkWithPassword("raw-link-token", "senha-errada"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}

package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.OauthIdentity;
import br.com.certamecards.auth.domain.OauthProvider;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OauthIdentityRepository;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleLoginHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final String EMAIL = "ana@exemplo.com";
    private static final String SUBJECT = "google-subject-123";

    private final OauthIdentityRepository oauthIdentityRepository = mock(OauthIdentityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GoogleLoginHandler handler =
            new GoogleLoginHandler(oauthIdentityRepository, userRepository, oneTimeTokenService, clock);

    @Test
    @DisplayName("TU-32 — identidade já vinculada conclui com o usuário existente")
    void givenIdentityAlreadyLinked_whenHandlingLogin_thenConcludesWithExistingUser() {
        UUID userId = UUID.randomUUID();
        OauthIdentity identity = new OauthIdentity(userId, OauthProvider.GOOGLE, SUBJECT, EMAIL);
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.of(identity));

        GoogleLoginOutcome outcome = handler.handleLogin(new GoogleProfile(EMAIL, SUBJECT, true, "Ana"));

        assertThat(outcome).isInstanceOf(GoogleLoginOutcome.Concluded.class);
        assertThat(((GoogleLoginOutcome.Concluded) outcome).userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("TU-32 — e-mail novo cria o usuário e conclui")
    void givenNoUserWithEmail_whenHandlingLogin_thenCreatesUserAndConcludes() {
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        GoogleLoginOutcome outcome = handler.handleLogin(new GoogleProfile(EMAIL, SUBJECT, true, "Ana"));

        assertThat(outcome).isInstanceOf(GoogleLoginOutcome.Concluded.class);
        verify(userRepository).save(any(User.class));
        verify(oauthIdentityRepository).save(any(OauthIdentity.class));
    }

    @Test
    void givenExistingUserWithoutPassword_whenHandlingLogin_thenLinksIdentityAndConcludes() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        GoogleLoginOutcome outcome = handler.handleLogin(new GoogleProfile(EMAIL, SUBJECT, true, "Ana"));

        assertThat(outcome).isInstanceOf(GoogleLoginOutcome.Concluded.class);
        assertThat(((GoogleLoginOutcome.Concluded) outcome).userId()).isEqualTo(user.getId());
        verify(oauthIdentityRepository).save(any(OauthIdentity.class));
    }

    @Test
    @DisplayName("TU-32 — e-mail com senha exige vincular antes de concluir")
    void givenExistingUserWithPassword_whenHandlingLogin_thenReturnsLinkRequiredToken() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed-password");
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(oneTimeTokenService.issueWithPayload(
                        user.getId(), OneTimeTokenPurpose.LINK_GOOGLE, GoogleTokenPayload.quote(SUBJECT)))
                .thenReturn(new IssuedToken("raw-link-token", NOW.plusSeconds(3600)));

        GoogleLoginOutcome outcome = handler.handleLogin(new GoogleProfile(EMAIL, SUBJECT, true, "Ana"));

        assertThat(outcome).isInstanceOf(GoogleLoginOutcome.LinkRequired.class);
        assertThat(((GoogleLoginOutcome.LinkRequired) outcome).rawToken()).isEqualTo("raw-link-token");
    }

    @Test
    void givenUnverifiedEmailNewUser_whenHandlingLogin_thenCreatesUserWithoutVerifiedEmail() {
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        handler.handleLogin(new GoogleProfile(EMAIL, SUBJECT, false, "Ana"));

        verify(userRepository)
                .save(org.mockito.ArgumentMatchers.argThat(created -> created.getEmailVerifiedAt() == null));
    }

    @Test
    void givenEmailWithMixedCaseAndSpaces_whenHandlingLogin_thenNormalizesBeforeLookup() {
        when(oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, SUBJECT))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        handler.handleLogin(new GoogleProfile("  Ana@Exemplo.com  ", SUBJECT, true, "Ana"));

        verify(userRepository).findByEmail(EMAIL);
    }
}

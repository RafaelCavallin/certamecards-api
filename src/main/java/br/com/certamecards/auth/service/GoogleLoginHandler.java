package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OauthIdentity;
import br.com.certamecards.auth.domain.OauthProvider;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OauthIdentityRepository;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleLoginHandler {

    private final OauthIdentityRepository oauthIdentityRepository;
    private final UserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final Clock clock;

    public GoogleLoginHandler(
            OauthIdentityRepository oauthIdentityRepository,
            UserRepository userRepository,
            OneTimeTokenService oneTimeTokenService,
            Clock clock) {
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.userRepository = userRepository;
        this.oneTimeTokenService = oneTimeTokenService;
        this.clock = clock;
    }

    @Transactional
    public GoogleLoginOutcome handleLogin(GoogleProfile profile) {
        Optional<OauthIdentity> linked =
                oauthIdentityRepository.findByProviderAndSubject(OauthProvider.GOOGLE, profile.subject());
        if (linked.isPresent()) {
            return new GoogleLoginOutcome.Concluded(linked.get().getUserId());
        }
        String email = normalize(profile.email());
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isEmpty()) {
            return new GoogleLoginOutcome.Concluded(
                    createUserAndLink(profile, email).getId());
        }
        User user = existing.get();
        if (user.getPasswordHash() == null) {
            linkIdentity(user, profile);
            return new GoogleLoginOutcome.Concluded(user.getId());
        }
        IssuedToken token = oneTimeTokenService.issueWithPayload(
                user.getId(), OneTimeTokenPurpose.LINK_GOOGLE, GoogleTokenPayload.quote(profile.subject()));
        return new GoogleLoginOutcome.LinkRequired(token.rawToken());
    }

    private User createUserAndLink(GoogleProfile profile, String email) {
        User user = new User(email, profile.displayName(), UserRole.CANDIDATE);
        if (profile.emailVerified()) {
            user.verifyEmail(clock.instant());
        }
        userRepository.save(user);
        linkIdentity(user, profile);
        return user;
    }

    private void linkIdentity(User user, GoogleProfile profile) {
        oauthIdentityRepository.save(new OauthIdentity(
                user.getId(), OauthProvider.GOOGLE, profile.subject(), user.getEmail(), clock.instant()));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}

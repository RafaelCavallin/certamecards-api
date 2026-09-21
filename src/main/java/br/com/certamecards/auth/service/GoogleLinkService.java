package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OauthIdentity;
import br.com.certamecards.auth.domain.OauthProvider;
import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.persistence.OauthIdentityRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleLinkService {

    private final OauthIdentityRepository oauthIdentityRepository;
    private final UserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public GoogleLinkService(
            OauthIdentityRepository oauthIdentityRepository,
            UserRepository userRepository,
            OneTimeTokenService oneTimeTokenService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.userRepository = userRepository;
        this.oneTimeTokenService = oneTimeTokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public String handleReauth(String subject) {
        OauthIdentity identity = oauthIdentityRepository
                .findByProviderAndSubject(OauthProvider.GOOGLE, subject)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        return oneTimeTokenService
                .issue(identity.getUserId(), OneTimeTokenPurpose.REAUTH)
                .rawToken();
    }

    @Transactional
    public User linkWithPassword(String rawLinkToken, String password) {
        OneTimeToken token = oneTimeTokenService.consume(rawLinkToken, OneTimeTokenPurpose.LINK_GOOGLE);
        User user = userRepository.findById(token.getUserId()).orElseThrow();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        String subject = GoogleTokenPayload.unquote(token.getPayload());
        oauthIdentityRepository.save(
                new OauthIdentity(user.getId(), OauthProvider.GOOGLE, subject, user.getEmail(), clock.instant()));
        return user;
    }
}

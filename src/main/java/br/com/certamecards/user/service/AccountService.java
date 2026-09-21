package br.com.certamecards.user.service;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.auth.service.OneTimeTokenService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.AccountPurgeQuery;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenService oneTimeTokenService;
    private final AccountPurgeQuery purgeQuery;
    private final Clock clock;

    public AccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OneTimeTokenService oneTimeTokenService,
            AccountPurgeQuery purgeQuery,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.oneTimeTokenService = oneTimeTokenService;
        this.purgeQuery = purgeQuery;
        this.clock = clock;
    }

    public User getProfile(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public User updateDisplayName(UUID userId, String displayName) {
        User user = getProfile(userId);
        user.changeDisplayName(displayName.strip());
        return user;
    }

    @Transactional
    public void acceptTerms(UUID userId, String version) {
        getProfile(userId).acceptTerms(version, clock.instant());
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountCommand command) {
        User user = getProfile(userId);
        ensureNotLastAdmin(user);
        verifyCredential(user, command);
        purgeQuery.purge(userId);
    }

    private void ensureNotLastAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw ApiException.of(ErrorCode.LAST_ADMIN);
        }
    }

    private void verifyCredential(User user, DeleteAccountCommand command) {
        if (command.reauthToken() != null) {
            verifyReauthToken(user, command.reauthToken());
            return;
        }
        if (command.password() == null || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void verifyReauthToken(User user, String rawToken) {
        OneTimeToken token = oneTimeTokenService.consume(rawToken, OneTimeTokenPurpose.REAUTH);
        if (!token.getUserId().equals(user.getId())) {
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}

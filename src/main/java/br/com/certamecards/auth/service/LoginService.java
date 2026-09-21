package br.com.certamecards.auth.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginThrottle loginThrottle;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final MeterRegistry meterRegistry;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginThrottle loginThrottle,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenService refreshTokenService,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginThrottle = loginThrottle;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public LoginResult login(LoginCommand command, String ip, String userAgent) {
        String email = normalize(command.email());
        loginThrottle.ensureAllowed(email);
        User user = authenticate(command, email, ip);
        if (user.getEmailVerifiedAt() == null) {
            meterRegistry
                    .counter("auth.login", "result", "unverified", "method", "password")
                    .increment();
            throw ApiException.of(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        loginThrottle.recordSuccess(email, ip);
        meterRegistry
                .counter("auth.login", "result", "success", "method", "password")
                .increment();
        return buildResult(user, userAgent);
    }

    private User authenticate(LoginCommand command, String email, String ip) {
        Optional<User> found = userRepository.findByEmail(email);
        boolean matches = found.filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(command.password(), u.getPasswordHash()))
                .isPresent();
        if (!matches) {
            loginThrottle.recordFailure(email, ip);
            meterRegistry
                    .counter("auth.login", "result", "invalid", "method", "password")
                    .increment();
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        return found.get();
    }

    private LoginResult buildResult(User user, String userAgent) {
        IssuedAccessToken access = accessTokenIssuer.issue(user.getId());
        IssuedRefreshToken refresh = refreshTokenService.issueNewFamily(user.getId(), userAgent);
        return new LoginResult(user, access, refresh);
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}

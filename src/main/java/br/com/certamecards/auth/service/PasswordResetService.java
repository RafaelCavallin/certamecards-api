package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendPasswordResetEmailEvent;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ClientOriginProperties originProperties;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetService(
            UserRepository userRepository,
            OneTimeTokenService oneTimeTokenService,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            ClientOriginProperties originProperties,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.oneTimeTokenService = oneTimeTokenService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.originProperties = originProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void forgot(String email) {
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(normalized).ifPresent(this::sendResetLink);
    }

    @Transactional
    public void reset(ResetPasswordCommand command) {
        OneTimeToken token = oneTimeTokenService.consume(command.rawToken(), OneTimeTokenPurpose.RESET_PASSWORD);
        User user = userRepository.findById(token.getUserId()).orElseThrow();
        user.changePasswordHash(passwordEncoder.encode(command.newPassword()));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private void sendResetLink(User user) {
        IssuedToken token = oneTimeTokenService.issue(user.getId(), OneTimeTokenPurpose.RESET_PASSWORD);
        String link = originProperties.frontendOrigin() + "/redefinir-senha#token=" + token.rawToken();
        eventPublisher.publishEvent(new SendPasswordResetEmailEvent(user.getEmail(), user.getDisplayName(), link));
    }
}

package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendConfirmationEmailEvent;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailConfirmationService {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final OneTimeTokenService oneTimeTokenService;
    private final ClientOriginProperties originProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Map<String, Instant> lastResendAt = new ConcurrentHashMap<>();

    public EmailConfirmationService(
            UserRepository userRepository,
            OneTimeTokenService oneTimeTokenService,
            ClientOriginProperties originProperties,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.userRepository = userRepository;
        this.oneTimeTokenService = oneTimeTokenService;
        this.originProperties = originProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void confirm(String rawToken) {
        OneTimeToken token = oneTimeTokenService.consume(rawToken, OneTimeTokenPurpose.CONFIRM_EMAIL);
        userRepository.findById(token.getUserId()).ifPresent(user -> user.verifyEmail(clock.instant()));
    }

    @Transactional
    public void resend(String email) {
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (isThrottled(normalized)) {
            return;
        }
        lastResendAt.put(normalized, clock.instant());
        userRepository
                .findByEmail(normalized)
                .filter(user -> user.getEmailVerifiedAt() == null)
                .ifPresent(this::send);
    }

    private void send(User user) {
        IssuedToken token = oneTimeTokenService.issue(user.getId(), OneTimeTokenPurpose.CONFIRM_EMAIL);
        String link = originProperties.frontendOrigin() + "/confirmar-email#token=" + token.rawToken();
        eventPublisher.publishEvent(new SendConfirmationEmailEvent(user.getEmail(), user.getDisplayName(), link));
    }

    private boolean isThrottled(String email) {
        Instant last = lastResendAt.get(email);
        return last != null && last.plus(RESEND_COOLDOWN).isAfter(clock.instant());
    }
}

package br.com.certamecards.auth.service;

import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendAccountExistsEmailEvent;
import br.com.certamecards.mail.SendConfirmationEmailEvent;
import br.com.certamecards.settings.service.UserSettingsService;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import br.com.certamecards.user.service.TermsProperties;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenService oneTimeTokenService;
    private final TermsProperties termsProperties;
    private final ClientOriginProperties originProperties;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(
            UserRepository userRepository,
            UserSettingsService userSettingsService,
            PasswordEncoder passwordEncoder,
            OneTimeTokenService oneTimeTokenService,
            TermsProperties termsProperties,
            ClientOriginProperties originProperties,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userSettingsService = userSettingsService;
        this.passwordEncoder = passwordEncoder;
        this.oneTimeTokenService = oneTimeTokenService;
        this.termsProperties = termsProperties;
        this.originProperties = originProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(RegisterCommand command) {
        if (!termsProperties.currentVersion().equals(command.acceptedTermsVersion())) {
            throw ApiException.withDetail(ErrorCode.VALIDATION_FAILED, "Versão dos termos desatualizada.");
        }
        String email = normalize(command.email());
        if (userRepository.findByEmail(email).isPresent()) {
            eventPublisher.publishEvent(new SendAccountExistsEmailEvent(email));
            return;
        }
        User user = createUser(command, email);
        userSettingsService.createDefault(user.getId(), command.timeZone());
        issueConfirmationEmail(user);
    }

    private User createUser(RegisterCommand command, String email) {
        User user = new User(email, command.displayName().strip(), UserRole.CANDIDATE);
        user.changePasswordHash(passwordEncoder.encode(command.password()));
        return userRepository.save(user);
    }

    private void issueConfirmationEmail(User user) {
        IssuedToken token = oneTimeTokenService.issue(user.getId(), OneTimeTokenPurpose.CONFIRM_EMAIL);
        String link = originProperties.frontendOrigin() + "/confirmar-email#token=" + token.rawToken();
        eventPublisher.publishEvent(new SendConfirmationEmailEvent(user.getEmail(), user.getDisplayName(), link));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}

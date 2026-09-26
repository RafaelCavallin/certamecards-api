package br.com.certamecards.auth.service;

import br.com.certamecards.settings.service.UserSettingsService;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmedAccountCreator {

    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;
    private final PasswordEncoder passwordEncoder;

    public ConfirmedAccountCreator(
            UserRepository userRepository, UserSettingsService userSettingsService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSettingsService = userSettingsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(CreateConfirmedAccountCommand command) {
        String email = command.email().strip().toLowerCase(Locale.ROOT);
        User user = new User(email, command.displayName().strip(), UserRole.CANDIDATE);
        user.changePasswordHash(passwordEncoder.encode(command.password()));
        user.verifyEmail(command.now());
        user.acceptTerms(command.termsVersion(), command.now());
        User saved = userRepository.save(user);
        userSettingsService.createDefault(saved.getId(), command.timeZone());
        return saved;
    }
}

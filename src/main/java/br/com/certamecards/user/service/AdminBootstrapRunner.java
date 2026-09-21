package br.com.certamecards.user.service;

import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AdminBootstrapProperties properties;

    public AdminBootstrapRunner(UserRepository userRepository, AdminBootstrapProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.bootstrapEmail())) {
            return;
        }
        userRepository
                .findByEmail(properties.bootstrapEmail().strip().toLowerCase())
                .ifPresent(this::promote);
    }

    private void promote(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            user.promoteToAdmin();
            userRepository.save(user);
        }
    }
}

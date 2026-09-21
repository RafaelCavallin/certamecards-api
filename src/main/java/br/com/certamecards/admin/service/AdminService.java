package br.com.certamecards.admin.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AdminAuditRecorder auditRecorder;

    public AdminService(UserRepository userRepository, AdminAuditRecorder auditRecorder) {
        this.userRepository = userRepository;
        this.auditRecorder = auditRecorder;
    }

    public List<User> listAdmins() {
        return userRepository.findAllByRole(UserRole.ADMIN);
    }

    @Transactional
    public User grant(UUID actorId, String rawEmail) {
        String email = rawEmail.strip().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        if (user.getRole() != UserRole.ADMIN) {
            user.promoteToAdmin();
            userRepository.save(user);
            auditRecorder.recordGranted(actorId, user);
        }
        return user;
    }

    @Transactional
    public void revoke(UUID actorId, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        ensureNotLastAdmin(user);
        user.demoteToCandidate();
        userRepository.save(user);
        auditRecorder.recordRevoked(actorId, user);
    }

    private void ensureNotLastAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw ApiException.of(ErrorCode.LAST_ADMIN);
        }
    }
}

package br.com.certamecards.admin.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.UserAuthCache;
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
    private final UserAuthCache userAuthCache;

    public AdminService(UserRepository userRepository, AdminAuditRecorder auditRecorder, UserAuthCache userAuthCache) {
        this.userRepository = userRepository;
        this.auditRecorder = auditRecorder;
        this.userAuthCache = userAuthCache;
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
            userAuthCache.evictAfterCommit(user.getId());
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
        userAuthCache.evictAfterCommit(user.getId());
    }

    private void ensureNotLastAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw ApiException.of(ErrorCode.LAST_ADMIN);
        }
    }
}

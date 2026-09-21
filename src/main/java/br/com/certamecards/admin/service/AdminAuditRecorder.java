package br.com.certamecards.admin.service;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.user.domain.User;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditRecorder {

    private final AdminAuditLogger auditLogger;

    public AdminAuditRecorder(AdminAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void recordGranted(UUID actorId, User user) {
        auditLogger.log(
                actorId,
                AuditAction.ADMIN_GRANTED,
                AuditTargetType.ADMIN_ROLE,
                user.getId(),
                user.getDisplayName(),
                Map.of("role", new AuditChange("candidate", "admin")));
    }

    public void recordRevoked(UUID actorId, User user) {
        auditLogger.log(
                actorId,
                AuditAction.ADMIN_REVOKED,
                AuditTargetType.ADMIN_ROLE,
                user.getId(),
                user.getDisplayName(),
                Map.of("role", new AuditChange("admin", "candidate")));
    }
}

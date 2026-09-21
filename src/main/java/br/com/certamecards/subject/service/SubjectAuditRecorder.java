package br.com.certamecards.subject.service;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.subject.domain.Subject;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubjectAuditRecorder {

    private final AdminAuditLogger auditLogger;

    public SubjectAuditRecorder(AdminAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void recordCreated(UUID actorId, Subject subject) {
        auditLogger.log(
                actorId,
                AuditAction.SUBJECT_CREATED,
                AuditTargetType.SUBJECT,
                subject.getId(),
                subject.getName(),
                Map.of("name", new AuditChange(null, subject.getName())));
    }

    public void recordRenamed(UUID actorId, Subject subject, String before, String after) {
        auditLogger.log(
                actorId,
                AuditAction.SUBJECT_RENAMED,
                AuditTargetType.SUBJECT,
                subject.getId(),
                subject.getName(),
                Map.of("name", new AuditChange(before, after)));
    }

    public void recordActiveChanged(UUID actorId, Subject subject, boolean before, boolean after) {
        AuditAction action = after ? AuditAction.SUBJECT_REACTIVATED : AuditAction.SUBJECT_DEACTIVATED;
        auditLogger.log(
                actorId,
                action,
                AuditTargetType.SUBJECT,
                subject.getId(),
                subject.getName(),
                Map.of("active", new AuditChange(before, after)));
    }
}

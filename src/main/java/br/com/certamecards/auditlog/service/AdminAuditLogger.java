package br.com.certamecards.auditlog.service;

import br.com.certamecards.auditlog.domain.AdminAuditLog;
import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.persistence.AdminAuditLogRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AdminAuditLogger {

    private final AdminAuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AdminAuditLogger(AdminAuditLogRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void log(
            UUID actorId,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            String targetLabel,
            Map<String, AuditChange> changes) {
        String changesJson = objectMapper.writeValueAsString(changes == null ? Map.of() : changes);
        AdminAuditLog entry = new AdminAuditLog(
                UUID.randomUUID(), actorId, action, targetType, targetId, targetLabel, changesJson, clock.instant());
        repository.save(entry);
    }
}

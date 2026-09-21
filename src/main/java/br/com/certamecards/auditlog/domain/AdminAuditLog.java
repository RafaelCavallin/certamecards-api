package br.com.certamecards.auditlog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "target_label")
    private String targetLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String changes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AdminAuditLog() {}

    public AdminAuditLog(
            UUID id,
            UUID actorId,
            AuditAction action,
            AuditTargetType targetType,
            UUID targetId,
            String targetLabel,
            String changes,
            Instant createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.action = action.code();
        this.targetType = targetType.code();
        this.targetId = targetId;
        this.targetLabel = targetLabel;
        this.changes = changes;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getChanges() {
        return changes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

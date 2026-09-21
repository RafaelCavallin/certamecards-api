package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.card.domain.Card;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficialCardAuditRecorder {

    private final AdminAuditLogger auditLogger;

    public OfficialCardAuditRecorder(AdminAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void recordCreated(UUID actorId, Card card, String label) {
        auditLogger.log(
                actorId,
                AuditAction.OFFICIAL_CARD_CREATED,
                AuditTargetType.OFFICIAL_CARD,
                card.getId(),
                label,
                Map.of());
    }

    public void recordUpdated(UUID actorId, Card card, String label, Map<String, AuditChange> changes) {
        if (!changes.isEmpty()) {
            auditLogger.log(
                    actorId,
                    AuditAction.OFFICIAL_CARD_UPDATED,
                    AuditTargetType.OFFICIAL_CARD,
                    card.getId(),
                    label,
                    changes);
        }
    }

    public void recordContentChanged(UUID actorId, Card card, String label, Map<String, AuditChange> changes) {
        auditLogger.log(
                actorId,
                AuditAction.OFFICIAL_CARD_CONTENT_CHANGED,
                AuditTargetType.OFFICIAL_CARD,
                card.getId(),
                label,
                changes);
    }

    public void recordDeleted(UUID actorId, UUID cardId, String label) {
        auditLogger.log(
                actorId, AuditAction.OFFICIAL_CARD_DELETED, AuditTargetType.OFFICIAL_CARD, cardId, label, Map.of());
    }
}

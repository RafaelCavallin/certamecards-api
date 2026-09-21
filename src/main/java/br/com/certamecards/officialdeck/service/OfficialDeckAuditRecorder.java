package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckAuditRecorder {

    private final AdminAuditLogger auditLogger;

    public OfficialDeckAuditRecorder(AdminAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void recordCreated(UUID actorId, Deck deck) {
        auditLogger.log(
                actorId,
                AuditAction.OFFICIAL_DECK_CREATED,
                AuditTargetType.OFFICIAL_DECK,
                deck.getId(),
                deck.getName(),
                Map.of());
    }

    public void recordUpdated(UUID actorId, Deck deck, Map<String, AuditChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        auditLogger.log(
                actorId,
                AuditAction.OFFICIAL_DECK_UPDATED,
                AuditTargetType.OFFICIAL_DECK,
                deck.getId(),
                deck.getName(),
                changes);
    }

    public void recordStatusChanged(UUID actorId, Deck deck, OfficialDeckStatus from, OfficialDeckStatus to) {
        auditLogger.log(
                actorId,
                statusChangeAction(to),
                AuditTargetType.OFFICIAL_DECK,
                deck.getId(),
                deck.getName(),
                Map.of("status", new AuditChange(from.code(), to.code())));
    }

    public void recordDeleted(UUID actorId, UUID deckId, String deckName) {
        auditLogger.log(
                actorId, AuditAction.OFFICIAL_DECK_DELETED, AuditTargetType.OFFICIAL_DECK, deckId, deckName, Map.of());
    }

    private AuditAction statusChangeAction(OfficialDeckStatus target) {
        return switch (target) {
            case PUBLISHED -> AuditAction.OFFICIAL_DECK_PUBLISHED;
            case DRAFT -> AuditAction.OFFICIAL_DECK_UNPUBLISHED;
            case DISCONTINUED -> AuditAction.OFFICIAL_DECK_DISCONTINUED;
        };
    }
}

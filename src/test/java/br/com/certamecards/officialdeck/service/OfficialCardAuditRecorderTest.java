package br.com.certamecards.officialdeck.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.card.domain.Card;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialCardAuditRecorderTest {

    private final AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
    private final OfficialCardAuditRecorder recorder = new OfficialCardAuditRecorder(auditLogger);
    private final UUID actorId = UUID.randomUUID();
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "F", "B");

    @Test
    void givenEmptyChanges_whenRecordingUpdated_thenDoesNotLog() {
        recorder.recordUpdated(actorId, card, "label", Map.of());

        verifyNoInteractions(auditLogger);
    }

    @Test
    void givenChanges_whenRecordingUpdated_thenLogsOfficialCardUpdated() {
        recorder.recordUpdated(actorId, card, "label", Map.of("back", new AuditChange("A", "B")));

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_CARD_UPDATED),
                        eq(AuditTargetType.OFFICIAL_CARD),
                        eq(card.getId()),
                        eq("label"),
                        any());
    }

    @Test
    void whenRecordingContentChanged_thenLogsOfficialCardContentChanged() {
        recorder.recordContentChanged(actorId, card, "label", Map.of("note", new AuditChange(null, "motivo")));

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_CARD_CONTENT_CHANGED),
                        eq(AuditTargetType.OFFICIAL_CARD),
                        eq(card.getId()),
                        eq("label"),
                        any());
    }

    @Test
    void whenRecordingCreatedAndDeleted_thenLogsBothActions() {
        recorder.recordCreated(actorId, card, "label");
        recorder.recordDeleted(actorId, card.getId(), "label");

        verify(auditLogger).log(eq(actorId), eq(AuditAction.OFFICIAL_CARD_CREATED), any(), any(), any(), any());
        verify(auditLogger).log(eq(actorId), eq(AuditAction.OFFICIAL_CARD_DELETED), any(), any(), any(), any());
    }
}

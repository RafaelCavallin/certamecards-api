package br.com.certamecards.officialdeck.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.certamecards.auditlog.domain.AuditAction;
import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.auditlog.domain.AuditTargetType;
import br.com.certamecards.auditlog.service.AdminAuditLogger;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckAuditRecorderTest {

    private final AdminAuditLogger auditLogger = mock(AdminAuditLogger.class);
    private final OfficialDeckAuditRecorder recorder = new OfficialDeckAuditRecorder(auditLogger);
    private final UUID actorId = UUID.randomUUID();
    private final Deck deck =
            new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);

    @Test
    void givenEmptyChanges_whenRecordingUpdated_thenDoesNotLog() {
        recorder.recordUpdated(actorId, deck, Map.of());

        verifyNoInteractions(auditLogger);
    }

    @Test
    void givenChanges_whenRecordingUpdated_thenLogsOfficialDeckUpdated() {
        recorder.recordUpdated(actorId, deck, Map.of("name", new AuditChange("A", "B")));

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_DECK_UPDATED),
                        eq(AuditTargetType.OFFICIAL_DECK),
                        eq(deck.getId()),
                        eq(deck.getName()),
                        any());
    }

    @Test
    void givenPublishTarget_whenRecordingStatusChanged_thenLogsPublishedAction() {
        recorder.recordStatusChanged(actorId, deck, OfficialDeckStatus.DRAFT, OfficialDeckStatus.PUBLISHED);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_DECK_PUBLISHED),
                        eq(AuditTargetType.OFFICIAL_DECK),
                        eq(deck.getId()),
                        eq(deck.getName()),
                        any());
    }

    @Test
    void givenDraftTarget_whenRecordingStatusChanged_thenLogsUnpublishedAction() {
        recorder.recordStatusChanged(actorId, deck, OfficialDeckStatus.PUBLISHED, OfficialDeckStatus.DRAFT);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_DECK_UNPUBLISHED),
                        eq(AuditTargetType.OFFICIAL_DECK),
                        eq(deck.getId()),
                        eq(deck.getName()),
                        any());
    }

    @Test
    void givenDiscontinuedTarget_whenRecordingStatusChanged_thenLogsDiscontinuedAction() {
        recorder.recordStatusChanged(actorId, deck, OfficialDeckStatus.PUBLISHED, OfficialDeckStatus.DISCONTINUED);

        verify(auditLogger)
                .log(
                        eq(actorId),
                        eq(AuditAction.OFFICIAL_DECK_DISCONTINUED),
                        eq(AuditTargetType.OFFICIAL_DECK),
                        eq(deck.getId()),
                        eq(deck.getName()),
                        any());
    }

    @Test
    void whenRecordingCreatedAndDeleted_thenLogsBothActions() {
        recorder.recordCreated(actorId, deck);
        recorder.recordDeleted(actorId, deck.getId(), deck.getName());

        verify(auditLogger).log(eq(actorId), eq(AuditAction.OFFICIAL_DECK_CREATED), any(), any(), any(), any());
        verify(auditLogger).log(eq(actorId), eq(AuditAction.OFFICIAL_DECK_DELETED), any(), any(), any(), any());
        verify(auditLogger, never())
                .log(eq(actorId), eq(AuditAction.OFFICIAL_DECK_UPDATED), any(), any(), any(), any());
    }
}

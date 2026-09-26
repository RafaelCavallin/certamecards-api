package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncConflict;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class ConflictRestoreHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();
    private static final UUID CONFLICT_ID = UUID.randomUUID();
    private final SyncConflictService conflictService = mock(SyncConflictService.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final DeckService deckService = mock(DeckService.class);
    private final CardService cardService = mock(CardService.class);
    private final ConflictRestoreHandler handler =
            new ConflictRestoreHandler(conflictService, payloadReader, deckService, cardService);

    @Test
    void givenDeckConflict_whenRestoring_thenRestoresDeckAndMarksConflictRestored() {
        when(conflictService.forRestore(USER_ID, CONFLICT_ID)).thenReturn(conflict("deck", null));
        Deck deck = mock(Deck.class);
        when(deck.getVersion()).thenReturn(2);
        when(deck.getChangeSeq()).thenReturn(9L);
        when(deckService.restore(any(), any(), any())).thenReturn(deck);
        SyncMutationOperation operation = restoreOperation(deckSnapshotPayload());

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.entityVersion()).isEqualTo(2);
        verify(conflictService).markRestored(CONFLICT_ID, order().eventAt());
    }

    @Test
    void givenCardConflictWithoutExplicitTarget_whenRestoring_thenUsesConflictDeckAsTarget() {
        UUID conflictDeckId = UUID.randomUUID();
        when(conflictService.forRestore(USER_ID, CONFLICT_ID)).thenReturn(conflict("card", conflictDeckId));
        Card card = mock(Card.class);
        when(card.getVersion()).thenReturn(1);
        when(card.getChangeSeq()).thenReturn(4L);
        when(cardService.restore(any(), any(), any(), any())).thenReturn(card);
        SyncMutationOperation operation = restoreOperation(cardSnapshotPayload(null));

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(cardService).restore(eq(USER_ID), eq(ENTITY_ID), eq(conflictDeckId), any());
    }

    @Test
    void givenConflictForDifferentEntity_whenRestoring_thenRejectsAsValidationFailed() {
        SyncConflict conflict = new SyncConflict(
                CONFLICT_ID,
                USER_ID,
                "deck",
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "concurrent_edit",
                null,
                null,
                NOW.plusSeconds(3600),
                null,
                null,
                NOW);
        when(conflictService.forRestore(USER_ID, CONFLICT_ID)).thenReturn(conflict);
        SyncMutationOperation operation = restoreOperation(deckSnapshotPayload());

        assertThatThrownBy(() -> handler.handle(USER_ID, new MutationContext(operation, order(), null)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    private SyncConflict conflict(String entityType, UUID deckId) {
        return new SyncConflict(
                CONFLICT_ID,
                USER_ID,
                entityType,
                ENTITY_ID,
                deckId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "concurrent_edit",
                null,
                null,
                NOW.plusSeconds(3600),
                null,
                null,
                NOW);
    }

    private ObjectNode deckSnapshotPayload() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("subjectId", UUID.randomUUID().toString());
        snapshot.put("name", "Restaurado");
        snapshot.put("description", "Descricao");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("conflictId", CONFLICT_ID.toString());
        payload.set("snapshot", snapshot);
        payload.putNull("targetDeckId");
        return payload;
    }

    private ObjectNode cardSnapshotPayload(UUID targetDeckId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("front", "Frente restaurada");
        snapshot.put("back", "Verso restaurado");
        snapshot.putNull("source");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("conflictId", CONFLICT_ID.toString());
        payload.set("snapshot", snapshot);
        if (targetDeckId == null) {
            payload.putNull("targetDeckId");
        } else {
            payload.put("targetDeckId", targetDeckId.toString());
        }
        return payload;
    }

    private SyncMutationOperation restoreOperation(ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.CONFLICT_RESTORE,
                ENTITY_ID,
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                payload);
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
    }
}

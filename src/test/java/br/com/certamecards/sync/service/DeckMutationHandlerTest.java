package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckCreationResult;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class DeckMutationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final DeckService deckService = mock(DeckService.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final DeckMutationHandler handler = new DeckMutationHandler(deckService, payloadReader, new ObjectMapper());

    @Test
    void givenCreateOperation_whenHandling_thenCreatesDeckAndReturnsApplied() {
        Deck deck = mock(Deck.class);
        when(deck.getVersion()).thenReturn(1);
        when(deck.getChangeSeq()).thenReturn(5L);
        when(deckService.create(any())).thenReturn(new DeckCreationResult(deck, true));
        SyncMutationOperation operation = deckOperation(SyncOperationKind.DECK_CREATE, payload());

        MutationResult result = handler.handle(USER_ID, context(operation, null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.entityVersion()).isEqualTo(1);
        assertThat(result.changeSeq()).isEqualTo(5L);
    }

    @Test
    void givenUpdateOperation_whenHandling_thenUpdatesDeckWithCurrentVersion() {
        Deck deck = mock(Deck.class);
        when(deck.getVersion()).thenReturn(2);
        when(deck.getChangeSeq()).thenReturn(6L);
        when(deckService.update(any(), any(), any())).thenReturn(deck);
        SyncMutationOperation operation = deckOperation(SyncOperationKind.DECK_UPDATE, payload());

        MutationResult result = handler.handle(USER_ID, context(operation, 1));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(deckService).update(any(), any(), any());
    }

    @Test
    void givenUpdateWithoutCurrentVersion_whenHandling_thenThrows() {
        SyncMutationOperation operation = deckOperation(SyncOperationKind.DECK_UPDATE, payload());

        assertThatThrownBy(() -> handler.handle(USER_ID, context(operation, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDeleteOperation_whenHandling_thenDeletesDeckWithCurrentVersion() {
        SyncMutationOperation operation = deckOperation(SyncOperationKind.DECK_DELETE, payload());

        MutationResult result = handler.handle(USER_ID, context(operation, 3));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(deckService).delete(USER_ID, operation.entityId(), 3);
    }

    @Test
    void givenUnsupportedKind_whenHandling_thenThrows() {
        SyncMutationOperation operation = deckOperation(SyncOperationKind.CARD_CREATE, payload());

        assertThatThrownBy(() -> handler.handle(USER_ID, context(operation, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ObjectNode payload() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("subjectId", UUID.randomUUID().toString());
        node.put("name", "Direito Constitucional");
        node.put("description", "Resumo");
        return node;
    }

    private SyncMutationOperation deckOperation(SyncOperationKind kind, ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                payload);
    }

    private MutationContext context(SyncMutationOperation operation, Integer currentVersion) {
        return new MutationContext(operation, order(), currentVersion);
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
    }
}

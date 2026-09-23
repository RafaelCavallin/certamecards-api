package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardCreationResult;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.sync.domain.EventClock;
import br.com.certamecards.sync.domain.EventOrder;
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

class CardMutationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final CardService cardService = mock(CardService.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final CardMutationHandler handler = new CardMutationHandler(cardService, payloadReader);

    @Test
    void givenCreateOperation_whenHandling_thenCreatesCard() {
        Card card = mock(Card.class);
        when(card.getVersion()).thenReturn(1);
        when(card.getChangeSeq()).thenReturn(9L);
        when(cardService.create(any())).thenReturn(new CardCreationResult(card, true));
        SyncMutationOperation operation = cardOperation(SyncOperationKind.CARD_CREATE, UUID.randomUUID());

        MutationResult result = handler.handle(USER_ID, context(operation, null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.entityVersion()).isEqualTo(1);
    }

    @Test
    void givenCreateWithoutParentId_whenHandling_thenThrows() {
        SyncMutationOperation operation = cardOperation(SyncOperationKind.CARD_CREATE, null);

        assertThatThrownBy(() -> handler.handle(USER_ID, context(operation, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenUpdateOperation_whenHandling_thenUpdatesCardWithCurrentVersion() {
        Card card = mock(Card.class);
        when(card.getVersion()).thenReturn(2);
        when(card.getChangeSeq()).thenReturn(10L);
        when(cardService.update(any(), any(), any())).thenReturn(card);
        SyncMutationOperation operation = cardOperation(SyncOperationKind.CARD_UPDATE, UUID.randomUUID());

        MutationResult result = handler.handle(USER_ID, context(operation, 1));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(cardService).update(any(), any(), any());
    }

    @Test
    void givenUpdateWithoutCurrentVersion_whenHandling_thenThrows() {
        SyncMutationOperation operation = cardOperation(SyncOperationKind.CARD_UPDATE, UUID.randomUUID());

        assertThatThrownBy(() -> handler.handle(USER_ID, context(operation, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDeleteOperation_whenHandling_thenDeletesCardWithCurrentVersion() {
        SyncMutationOperation operation = cardOperation(SyncOperationKind.CARD_DELETE, UUID.randomUUID());

        MutationResult result = handler.handle(USER_ID, context(operation, 4));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(cardService).delete(USER_ID, operation.entityId(), 4);
    }

    @Test
    void givenUnsupportedKind_whenHandling_thenThrows() {
        SyncMutationOperation operation = cardOperation(SyncOperationKind.DECK_CREATE, UUID.randomUUID());

        assertThatThrownBy(() -> handler.handle(USER_ID, context(operation, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SyncMutationOperation cardOperation(SyncOperationKind kind, UUID parentId) {
        ObjectNode payload = new ObjectMapper().createObjectNode();
        payload.put("front", "Pergunta");
        payload.put("back", "Resposta");
        payload.putNull("source");
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
                UUID.randomUUID(),
                parentId,
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

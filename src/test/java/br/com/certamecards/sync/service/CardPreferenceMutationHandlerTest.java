package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.service.CardService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.DeckProgressResetter;
import br.com.certamecards.deck.service.ResetProgressResult;
import br.com.certamecards.review.domain.CardState;
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

class CardPreferenceMutationHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();
    private final CardService cardService = mock(CardService.class);
    private final DeckProgressResetter progressResetter = mock(DeckProgressResetter.class);
    private final MutationPayloadReader payloadReader = new MutationPayloadReader(new ObjectMapper());
    private final CardPreferenceMutationHandler handler =
            new CardPreferenceMutationHandler(cardService, progressResetter, payloadReader);

    @Test
    void givenSuspensionOperation_whenHandling_thenSuspendsCard() {
        CardState state = mock(CardState.class);
        when(state.getChangeSeq()).thenReturn(7L);
        when(cardService.setSuspension(USER_ID, entityId(), true)).thenReturn(state);
        ObjectNode payload = new ObjectMapper().createObjectNode();
        payload.put("suspended", true);
        SyncMutationOperation operation = operation(SyncOperationKind.CARD_SUSPENSION, payload);

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.changeSeq()).isEqualTo(7L);
    }

    @Test
    void givenResetOperation_whenHandling_thenResetsProgress() {
        when(progressResetter.reset(USER_ID, entityId())).thenReturn(new ResetProgressResult(3, 42L));
        SyncMutationOperation operation =
                operation(SyncOperationKind.DECK_RESET, new ObjectMapper().createObjectNode());

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.changeSeq()).isEqualTo(42L);
    }

    @Test
    void givenSuspensionOnUnavailableCard_whenHandling_thenReturnsNotApplicable() {
        when(cardService.setSuspension(USER_ID, entityId(), true)).thenThrow(ApiException.of(ErrorCode.NOT_FOUND));
        ObjectNode payload = new ObjectMapper().createObjectNode();
        payload.put("suspended", true);
        SyncMutationOperation operation = operation(SyncOperationKind.CARD_SUSPENSION, payload);

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("not_applicable");
    }

    @Test
    void givenResetOnUnavailableDeck_whenHandling_thenReturnsNotApplicable() {
        when(progressResetter.reset(USER_ID, entityId())).thenThrow(ApiException.of(ErrorCode.NOT_FOUND));
        SyncMutationOperation operation =
                operation(SyncOperationKind.DECK_RESET, new ObjectMapper().createObjectNode());

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order(), null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("not_applicable");
    }

    @Test
    void givenSuspensionFailsWithOtherError_whenHandling_thenRethrows() {
        when(cardService.setSuspension(USER_ID, entityId(), true)).thenThrow(ApiException.of(ErrorCode.FORBIDDEN));
        ObjectNode payload = new ObjectMapper().createObjectNode();
        payload.put("suspended", true);
        SyncMutationOperation operation = operation(SyncOperationKind.CARD_SUSPENSION, payload);

        assertThatThrownBy(() -> handler.handle(USER_ID, new MutationContext(operation, order(), null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void givenUnsupportedKind_whenHandling_thenThrows() {
        SyncMutationOperation operation =
                operation(SyncOperationKind.DECK_CREATE, new ObjectMapper().createObjectNode());

        assertThatThrownBy(() -> handler.handle(USER_ID, new MutationContext(operation, order(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static UUID entityId() {
        return ENTITY_ID;
    }

    private SyncMutationOperation operation(SyncOperationKind kind, ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
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

package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.DeckResetLogWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DeckResetHandlerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DECK_ID = UUID.randomUUID();
    private final DeckService deckService = mock(DeckService.class);
    private final DeckResetLogWriter resetLogWriter = mock(DeckResetLogWriter.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final DeckResetHandler handler = new DeckResetHandler(deckService, resetLogWriter, clock);

    @Test
    void givenAccessibleDeck_whenHandling_thenMaterializesResetFactsWithCanonicalOrder() {
        when(deckService.findAccessible(USER_ID, DECK_ID)).thenReturn(mock(Deck.class));
        SyncMutationOperation operation = operation();
        EventOrder order = order();

        MutationResult result = handler.handle(USER_ID, new MutationContext(operation, order, null));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(resetLogWriter).materialize(USER_ID, DECK_ID, order, NOW);
    }

    @Test
    void givenUnavailableDeck_whenHandling_thenPropagatesForCoordinatorToTranslate() {
        when(deckService.findAccessible(USER_ID, DECK_ID)).thenThrow(ApiException.of(ErrorCode.NOT_FOUND));
        SyncMutationOperation operation = operation();
        MutationContext context = new MutationContext(operation, order(), null);

        assertThatThrownBy(() -> handler.handle(USER_ID, context)).isInstanceOf(ApiException.class);
        verify(resetLogWriter, never()).materialize(any(), any(), any(), any());
    }

    private SyncMutationOperation operation() {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_RESET,
                DECK_ID,
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                new ObjectMapper().createObjectNode());
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
    }
}

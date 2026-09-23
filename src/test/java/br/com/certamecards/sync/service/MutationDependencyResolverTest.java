package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.sync.domain.EventClock;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class MutationDependencyResolverTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final MutationDependencyResolver resolver = new MutationDependencyResolver();

    @Test
    void givenCardDependingOnDeck_whenOrdering_thenPlacesDeckFirst() {
        UUID deckOperationId = UUID.randomUUID();
        SyncMutationOperation deck = operation(deckOperationId, List.of());
        SyncMutationOperation card = operation(UUID.randomUUID(), List.of(deckOperationId));

        List<SyncMutationOperation> ordered = resolver.order(List.of(card, deck));

        assertThat(ordered).containsExactly(deck, card);
    }

    @Test
    void givenDuplicateOperationId_whenOrdering_thenRejectsBatch() {
        UUID operationId = UUID.randomUUID();
        SyncMutationOperation first = operation(operationId, List.of());
        SyncMutationOperation second = operation(operationId, List.of());

        assertThatThrownBy(() -> resolver.order(List.of(first, second))).isInstanceOf(ApiException.class);
    }

    @Test
    void givenDependencyCycle_whenOrdering_thenRejectsBatch() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        SyncMutationOperation first = operation(firstId, List.of(secondId));
        SyncMutationOperation second = operation(secondId, List.of(firstId));

        assertThatThrownBy(() -> resolver.order(List.of(first, second))).isInstanceOf(ApiException.class);
    }

    private SyncMutationOperation operation(UUID operationId, List<UUID> dependencies) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                dependencies,
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }
}

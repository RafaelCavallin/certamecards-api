package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardAccessResolver;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.review.domain.CardStateSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewPushContextResolverTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final UUID DEVICE_ID = UUID.randomUUID();
    private static final UUID DECK_ID = UUID.randomUUID();

    private final CardAccessResolver cardAccessResolver = mock(CardAccessResolver.class);
    private final ReviewPushContextResolver resolver = new ReviewPushContextResolver(cardAccessResolver, FIXED_CLOCK);

    @Test
    @DisplayName("TU-27 — junta os ids de cartões de reviews e de estados e usa o relógio para 'now'")
    void givenReviewsAndStates_whenResolving_thenCollectsBothCardIdsAndUsesClock() {
        UUID userId = UUID.randomUUID();
        UUID reviewCardId = UUID.randomUUID();
        UUID stateCardId = UUID.randomUUID();
        Card reviewCard = new Card(reviewCardId, DECK_ID, "Frente", "Verso");
        Card stateCard = new Card(stateCardId, DECK_ID, "Frente 2", "Verso 2");
        when(cardAccessResolver.resolveAllForRead(eq(userId), any()))
                .thenReturn(Map.of(reviewCardId, reviewCard, stateCardId, stateCard));

        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(),
                reviewCardId,
                "review",
                (short) 3,
                FIXED_NOW,
                5_000,
                null,
                "{}",
                true,
                DEVICE_ID,
                null,
                new EventClock(FIXED_NOW, 0),
                FIXED_NOW);
        CardStateSnapshot snapshot =
                new CardStateSnapshot(2, 4.2, 5.1, FIXED_NOW.plusSeconds(3600), FIXED_NOW, 3, 0, 0, 4);
        CardStatePushInput state = new CardStatePushInput(stateCardId, snapshot, 3);
        ReviewPushCommand command = new ReviewPushCommand(DEVICE_ID, List.of(review), List.of(), List.of(state));

        ReviewPushContext context = resolver.resolve(userId, command);

        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.now()).isEqualTo(FIXED_NOW);
        assertThat(context.accessibleCards()).containsOnlyKeys(reviewCardId, stateCardId);
        verify(cardAccessResolver).resolveAllForRead(eq(userId), eq(Set.of(reviewCardId, stateCardId)));
    }
}

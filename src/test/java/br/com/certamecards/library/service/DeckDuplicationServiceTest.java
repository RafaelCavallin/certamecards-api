package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.library.domain.DuplicateDeckCommand;
import br.com.certamecards.library.domain.DuplicationCounts;
import br.com.certamecards.library.domain.DuplicationOptions;
import br.com.certamecards.library.domain.DuplicationPlan;
import br.com.certamecards.library.domain.DuplicationSource;
import br.com.certamecards.library.persistence.DeckCopyWriter;
import br.com.certamecards.library.persistence.UserCardCountsQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeckDuplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T14:00:00Z");

    private final DeckCopyWriter copyWriter = mock(DeckCopyWriter.class);
    private final DuplicationSourceLoader sourceLoader = mock(DuplicationSourceLoader.class);
    private final DuplicationPlanner planner = mock(DuplicationPlanner.class);
    private final UserCardCountsQuery countsQuery = mock(UserCardCountsQuery.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final DeckDuplicationService service = new DeckDuplicationService(
            copyWriter, sourceLoader, planner, countsQuery, subscriptionService, Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID userId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();
    private final UUID copyId = UUID.randomUUID();
    private final Deck copy = new Deck(copyId, userId, UUID.randomUUID(), "Cópia", DeckOrigin.OFFICIAL_COPY);
    private final DuplicationCounts counts = new DuplicationCounts(5, 2, 10);

    @Test
    void givenCopyAlreadyExists_whenDuplicating_thenReturnsItWithoutCopyingAgain() {
        when(copyWriter.findExisting(userId, copyId)).thenReturn(Optional.of(copy));
        when(copyWriter.describe(userId, copy)).thenReturn(counts);

        DuplicationResult result = service.duplicate(command(true, false));

        assertThat(result.created()).isFalse();
        assertThat(result.counts()).isEqualTo(counts);
        verify(copyWriter, never()).copy(any(), any());
    }

    @Test
    void givenNewCopyWithoutCancellation_whenDuplicating_thenCopiesAndKeepsSubscription() {
        stubNewCopy(new DuplicationPlan(true, false));

        DuplicationResult result = service.duplicate(command(true, false));

        assertThat(result.created()).isTrue();
        assertThat(result.deck()).isSameAs(copy);
        verify(subscriptionService, never()).cancel(any(), any());
    }

    @Test
    void givenPlanToCancel_whenDuplicating_thenCancelsSubscriptionAfterCopying() {
        stubNewCopy(new DuplicationPlan(true, true));

        service.duplicate(command(true, true));

        verify(subscriptionService).cancel(userId, sourceId);
    }

    private void stubNewCopy(DuplicationPlan plan) {
        DuplicationSource source = new DuplicationSource(copy, true);
        when(copyWriter.findExisting(userId, copyId)).thenReturn(Optional.empty());
        when(sourceLoader.load(userId, sourceId)).thenReturn(source);
        when(countsQuery.usedCards(userId)).thenReturn(100L);
        when(planner.plan(any(), any(), org.mockito.ArgumentMatchers.eq(100L))).thenReturn(plan);
        when(copyWriter.copy(any(), org.mockito.ArgumentMatchers.eq(NOW))).thenReturn(counts);
        when(copyWriter.load(copyId)).thenReturn(copy);
    }

    private DuplicateDeckCommand command(boolean carry, boolean cancel) {
        return new DuplicateDeckCommand(userId, sourceId, new DuplicationOptions(copyId, carry, cancel));
    }
}

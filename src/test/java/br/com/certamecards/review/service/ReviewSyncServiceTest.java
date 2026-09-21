package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardAccessResolver;
import br.com.certamecards.review.domain.AppliedState;
import br.com.certamecards.review.domain.CardStateSnapshot;
import br.com.certamecards.review.domain.IgnoredState;
import br.com.certamecards.review.domain.RejectedReview;
import br.com.certamecards.review.domain.ReviewPushResult;
import br.com.certamecards.review.domain.ReviewSyncLimits;
import br.com.certamecards.review.domain.StaleState;
import br.com.certamecards.review.persistence.CardStateUpsertWriter;
import br.com.certamecards.review.persistence.ContentUpdateNoticeClearer;
import br.com.certamecards.review.persistence.ReviewLogRepository;
import br.com.certamecards.review.persistence.ReviewLogWriter;
import br.com.certamecards.review.persistence.ReviewVoidWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReviewSyncServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final UUID DECK_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();

    private final CardAccessResolver cardAccessResolver = mock(CardAccessResolver.class);
    private final ReviewLogWriter reviewLogWriter = mock(ReviewLogWriter.class);
    private final ReviewLogRepository reviewLogRepository = mock(ReviewLogRepository.class);
    private final CardStateUpsertWriter cardStateUpsertWriter = mock(CardStateUpsertWriter.class);
    private final ReviewVoidWriter voidWriter = mock(ReviewVoidWriter.class);
    private final ContentUpdateNoticeClearer noticeClearer = mock(ContentUpdateNoticeClearer.class);
    private ReviewSyncService service;

    @BeforeEach
    void setUp() {
        ReviewWriter reviewWriter = new ReviewWriter(new ReviewLogValidator(), reviewLogWriter, noticeClearer);
        CardStateReconciler reconciler = new CardStateReconciler(reviewLogRepository, cardStateUpsertWriter);
        var metrics = new ReviewSyncMetricsRecorder(new SimpleMeterRegistry());
        service = new ReviewSyncService(reviewWriter, voidWriter, reconciler, cardAccessResolver, metrics, FIXED_CLOCK);
        when(voidWriter.insertAll(any(), any())).thenReturn(List.of());
        doNothing().when(reviewLogWriter).insertAll(any(), any(), any());
    }

    @Test
    void givenMatchingReviewCount_whenPushingState_thenStateIsApplied() {
        UUID cardId = UUID.randomUUID();
        givenOwnedCard(cardId, false);
        when(reviewLogRepository.countAcceptedByCardIdAndUserId(any(), any())).thenReturn(3L);
        when(cardStateUpsertWriter.upsert(any(), any(), any(), anyInt(), any())).thenReturn(77L);
        ReviewPushCommand command = commandWithState(cardId, 3);

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.appliedStates()).containsExactly(new AppliedState(cardId, 77L));
        assertThat(result.staleStates()).isEmpty();
        assertThat(result.ignoredStates()).isEmpty();
    }

    @Test
    void givenDifferentReviewCount_whenPushingState_thenStateIsStaleWithServerCount() {
        UUID cardId = UUID.randomUUID();
        givenOwnedCard(cardId, false);
        when(reviewLogRepository.countAcceptedByCardIdAndUserId(any(), any())).thenReturn(5L);
        ReviewPushCommand command = commandWithState(cardId, 4);

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.staleStates()).containsExactly(new StaleState(cardId, 5L));
        assertThat(result.appliedStates()).isEmpty();
    }

    @Test
    void givenDeletedCard_whenPushingState_thenStateIsIgnoredWithCardDeleted() {
        UUID cardId = UUID.randomUUID();
        givenOwnedCard(cardId, true);
        ReviewPushCommand command = commandWithState(cardId, 0);

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.ignoredStates()).containsExactly(new IgnoredState(cardId, "card_deleted"));
    }

    @Test
    void givenFutureReviewedAt_whenPushingReview_thenReviewIsRejectedAsFutureTimestamp() {
        UUID cardId = UUID.randomUUID();
        givenOwnedCard(cardId, false);
        ReviewLogInput review = reviewInput(cardId, FIXED_NOW.plusSeconds(360), 5_000);
        ReviewPushCommand command = new ReviewPushCommand(DEVICE_ID, List.of(review), List.of(), List.of());

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.rejectedReviews()).containsExactly(new RejectedReview(review.id(), "future_timestamp"));
        assertThat(result.acceptedReviewIds()).isEmpty();
    }

    @Test
    void givenDurationOutOfRange_whenPushingReview_thenReviewIsRejectedAsInvalid() {
        UUID cardId = UUID.randomUUID();
        givenOwnedCard(cardId, false);
        ReviewLogInput review = reviewInput(cardId, FIXED_NOW.minusSeconds(60), ReviewSyncLimits.MAX_DURATION_MS + 1);
        ReviewPushCommand command = new ReviewPushCommand(DEVICE_ID, List.of(review), List.of(), List.of());

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.rejectedReviews()).containsExactly(new RejectedReview(review.id(), "invalid_review"));
    }

    @Test
    void givenNoReviewsOrStates_whenPushingOnlyVoids_thenDoesNotLookUpCards() {
        ReviewPushCommand command = new ReviewPushCommand(
                DEVICE_ID, List.of(), List.of(new ReviewVoidInput(UUID.randomUUID(), FIXED_NOW)), List.of());

        ReviewPushResult result = service.push(UUID.randomUUID(), command);

        assertThat(result.acceptedReviewIds()).isEmpty();
        assertThat(result.appliedStates()).isEmpty();
    }

    private void givenOwnedCard(UUID cardId, boolean deleted) {
        Card card = new Card(cardId, DECK_ID, "Frente", "Verso");
        if (deleted) {
            card.markDeleted(FIXED_NOW);
        }
        when(cardAccessResolver.resolveAllForRead(any(), any())).thenReturn(Map.of(cardId, card));
    }

    private ReviewPushCommand commandWithState(UUID cardId, int reviewCount) {
        CardStateSnapshot snapshot =
                new CardStateSnapshot(2, 4.2, 5.1, FIXED_NOW.plusSeconds(3600), FIXED_NOW, 3, 0, 0, 4);
        CardStatePushInput state = new CardStatePushInput(cardId, snapshot, reviewCount);
        return new ReviewPushCommand(DEVICE_ID, List.of(), List.of(), List.of(state));
    }

    private ReviewLogInput reviewInput(UUID cardId, Instant reviewedAt, int durationMs) {
        return new ReviewLogInput(
                UUID.randomUUID(),
                cardId,
                "review",
                (short) 3,
                reviewedAt,
                durationMs,
                null,
                "{}",
                true,
                DEVICE_ID,
                null);
    }
}

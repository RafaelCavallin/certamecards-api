package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardAccessResolver;
import br.com.certamecards.review.domain.ReviewPushResult;
import br.com.certamecards.review.persistence.ReviewVoidWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewSyncService {

    private final ReviewWriter reviewWriter;
    private final ReviewVoidWriter voidWriter;
    private final CardStateReconciler stateReconciler;
    private final CardAccessResolver cardAccessResolver;
    private final ReviewSyncMetricsRecorder metricsRecorder;
    private final Clock clock;

    public ReviewSyncService(
            ReviewWriter reviewWriter,
            ReviewVoidWriter voidWriter,
            CardStateReconciler stateReconciler,
            CardAccessResolver cardAccessResolver,
            ReviewSyncMetricsRecorder metricsRecorder,
            Clock clock) {
        this.reviewWriter = reviewWriter;
        this.voidWriter = voidWriter;
        this.stateReconciler = stateReconciler;
        this.cardAccessResolver = cardAccessResolver;
        this.metricsRecorder = metricsRecorder;
        this.clock = clock;
    }

    @Transactional
    public ReviewPushResult push(UUID userId, ReviewPushCommand command) {
        Instant now = clock.instant();
        Map<UUID, Card> accessibleCards = accessibleCards(userId, command);
        ReviewValidationOutcome reviewOutcome = reviewWriter.insertAll(userId, command.reviews(), accessibleCards, now);
        List<UUID> acceptedVoids = voidWriter.insertAll(userId, command.voids());
        StateOutcome stateOutcome = stateReconciler.reconcile(
                new StateReconciliationRequest(userId, command.states(), accessibleCards, now));
        metricsRecorder.record(new ReviewMetricsInput(
                reviewOutcome.valid(), reviewOutcome.rejected(), acceptedVoids, stateOutcome, now));
        return toResult(reviewOutcome, acceptedVoids, stateOutcome);
    }

    private Map<UUID, Card> accessibleCards(UUID userId, ReviewPushCommand command) {
        Set<UUID> ids = collectCardIds(command);
        return cardAccessResolver.resolveAllForRead(userId, ids);
    }

    private Set<UUID> collectCardIds(ReviewPushCommand command) {
        Set<UUID> ids = new HashSet<>();
        command.reviews().forEach(review -> ids.add(review.cardId()));
        command.states().forEach(state -> ids.add(state.cardId()));
        return ids;
    }

    private ReviewPushResult toResult(
            ReviewValidationOutcome reviewOutcome, List<UUID> acceptedVoids, StateOutcome stateOutcome) {
        List<UUID> acceptedReviewIds =
                reviewOutcome.valid().stream().map(ReviewLogInput::id).toList();
        return new ReviewPushResult(
                acceptedReviewIds,
                reviewOutcome.rejected(),
                acceptedVoids,
                stateOutcome.applied(),
                stateOutcome.stale(),
                stateOutcome.ignored());
    }
}

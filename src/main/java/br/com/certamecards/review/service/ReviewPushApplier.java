package br.com.certamecards.review.service;

import br.com.certamecards.review.persistence.ReviewVoidWriter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewPushApplier {

    private final ReviewWriter reviewWriter;
    private final ReviewVoidWriter voidWriter;
    private final CardStateReconciler stateReconciler;

    public ReviewPushApplier(
            ReviewWriter reviewWriter, ReviewVoidWriter voidWriter, CardStateReconciler stateReconciler) {
        this.reviewWriter = reviewWriter;
        this.voidWriter = voidWriter;
        this.stateReconciler = stateReconciler;
    }

    public ReviewPushOutcome apply(ReviewPushContext context, ReviewPushCommand command) {
        ReviewInsertOutcome reviewOutcome =
                reviewWriter.insertAll(context.userId(), command.reviews(), context.accessibleCards(), context.now());
        List<UUID> acceptedVoids = voidWriter.insertAll(context.userId(), command.voids());
        StateOutcome stateOutcome = stateReconciler.reconcile(new StateReconciliationRequest(
                context.userId(), command.states(), context.accessibleCards(), context.now()));
        return new ReviewPushOutcome(reviewOutcome, acceptedVoids, stateOutcome);
    }
}

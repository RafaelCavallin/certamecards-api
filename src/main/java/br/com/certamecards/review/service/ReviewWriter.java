package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.domain.AcceptedReview;
import br.com.certamecards.review.persistence.ContentUpdateNoticeClearer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewWriter {

    private final ReviewLogValidator validator;
    private final ReviewLogAcceptor acceptor;
    private final ContentUpdateNoticeClearer noticeClearer;

    public ReviewWriter(
            ReviewLogValidator validator, ReviewLogAcceptor acceptor, ContentUpdateNoticeClearer noticeClearer) {
        this.validator = validator;
        this.acceptor = acceptor;
        this.noticeClearer = noticeClearer;
    }

    public ReviewInsertOutcome insertAll(
            UUID userId, List<ReviewLogInput> reviews, Map<UUID, Card> ownedCards, Instant now) {
        ReviewValidationOutcome outcome = validator.validate(reviews, ownedCards);
        List<AcceptedReview> accepted = outcome.valid().stream()
                .map(review -> acceptor.accept(userId, review, now))
                .toList();
        noticeClearer.clearFor(userId, outcome.valid());
        return new ReviewInsertOutcome(outcome.valid(), accepted, outcome.rejected());
    }
}

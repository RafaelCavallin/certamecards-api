package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.persistence.ContentUpdateNoticeClearer;
import br.com.certamecards.review.persistence.ReviewLogWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewWriter {

    private final ReviewLogValidator validator;
    private final ReviewLogWriter reviewLogWriter;
    private final ContentUpdateNoticeClearer noticeClearer;

    public ReviewWriter(
            ReviewLogValidator validator, ReviewLogWriter reviewLogWriter, ContentUpdateNoticeClearer noticeClearer) {
        this.validator = validator;
        this.reviewLogWriter = reviewLogWriter;
        this.noticeClearer = noticeClearer;
    }

    public ReviewValidationOutcome insertAll(
            UUID userId, List<ReviewLogInput> reviews, Map<UUID, Card> ownedCards, Instant now) {
        ReviewValidationOutcome outcome = validator.validate(reviews, ownedCards, now);
        reviewLogWriter.insertAll(userId, outcome.valid(), now);
        noticeClearer.clearFor(userId, outcome.valid());
        return outcome;
    }
}

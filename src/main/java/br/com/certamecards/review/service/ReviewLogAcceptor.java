package br.com.certamecards.review.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.common.sync.EventOrderNormalizer;
import br.com.certamecards.review.domain.AcceptedReview;
import br.com.certamecards.review.persistence.ReviewLogWriter;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewLogAcceptor {

    private final ReviewLogWriter reviewLogWriter;
    private final EventOrderNormalizer orderNormalizer;

    public ReviewLogAcceptor(ReviewLogWriter reviewLogWriter, EventOrderNormalizer orderNormalizer) {
        this.reviewLogWriter = reviewLogWriter;
        this.orderNormalizer = orderNormalizer;
    }

    public AcceptedReview accept(UUID userId, ReviewLogInput review, Instant now) {
        EventOrder order = orderNormalizer.normalize(
                review.clock(), review.deviceId(), review.id(), review.observedServerTime(), now);
        reviewLogWriter.insertOne(userId, review, order, now);
        return new AcceptedReview(review.id(), order);
    }
}

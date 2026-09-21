package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewVoidTest {

    @Test
    void givenReviewIdAndVoidedAt_whenConstructing_thenGettersReturnValues() {
        UUID reviewId = UUID.randomUUID();
        Instant voidedAt = Instant.parse("2026-09-17T12:10:04Z");
        ReviewVoid reviewVoid = new ReviewVoid(reviewId, voidedAt);
        assertThat(reviewVoid.getReviewId()).isEqualTo(reviewId);
        assertThat(reviewVoid.getVoidedAt()).isEqualTo(voidedAt);
        assertThat(reviewVoid.getChangeSeq()).isNull();
    }
}

package br.com.certamecards.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewVoidChangeTest {

    @Test
    void givenFields_whenConstructing_thenAccessorsReturnValues() {
        UUID reviewId = UUID.randomUUID();
        Instant voidedAt = Instant.parse("2026-09-17T12:10:04Z");

        ReviewVoidChange change = new ReviewVoidChange(reviewId, voidedAt, 42L);

        assertThat(change.reviewId()).isEqualTo(reviewId);
        assertThat(change.voidedAt()).isEqualTo(voidedAt);
        assertThat(change.changeSeq()).isEqualTo(42L);
    }
}

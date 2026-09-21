package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewLogTest {

    @Test
    void givenReviewFields_whenConstructing_thenGettersReturnValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Instant reviewedAt = Instant.parse("2026-09-17T12:10:00Z");
        ReviewLog log = new ReviewLog(id, userId, cardId, ReviewKind.REVIEW, reviewedAt, deviceId, "{}");
        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getCardId()).isEqualTo(cardId);
        assertThat(log.getKind()).isEqualTo(ReviewKind.REVIEW);
        assertThat(log.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(log.getStateAfter()).isEqualTo("{}");
        assertThat(log.getSubmission().getDeviceId()).isEqualTo(deviceId);
        assertThat(log.getRating()).isNull();
        assertThat(log.getStateBefore()).isNull();
        assertThat(log.getDurationMs()).isZero();
        assertThat(log.getChangeSeq()).isNull();
    }

    @Test
    void givenLog_whenAssigningStateBefore_thenGetterReturnsAssignedValue() {
        ReviewLog log = new ReviewLog(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReviewKind.RESET,
                Instant.parse("2026-09-17T12:10:00Z"),
                UUID.randomUUID(),
                "{}");

        log.assignStateBefore("{\"state\":2}");

        assertThat(log.getStateBefore()).isEqualTo("{\"state\":2}");
    }
}

package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewSubmissionTest {

    @Test
    void givenDeviceId_whenConstructing_thenNotOfflineAndNoSession() {
        UUID deviceId = UUID.randomUUID();
        ReviewSubmission submission = new ReviewSubmission(deviceId);
        assertThat(submission.getDeviceId()).isEqualTo(deviceId);
        assertThat(submission.isOffline()).isFalse();
        assertThat(submission.getSessionId()).isNull();
        assertThat(submission.getReceivedAt()).isNull();
    }

    @Test
    void givenSubmission_whenMarkingReceived_thenReceivedAtIsSet() {
        ReviewSubmission submission = new ReviewSubmission(UUID.randomUUID());
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        submission.markReceived(now);
        assertThat(submission.getReceivedAt()).isEqualTo(now);
    }
}

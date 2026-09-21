package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FsrsProgressTest {

    @Test
    void givenDue_whenConstructing_thenStateIsNewAndFieldsAreZero() {
        Instant due = Instant.parse("2026-09-17T12:00:00Z");
        FsrsProgress progress = new FsrsProgress(due);
        assertThat(progress.getDue()).isEqualTo(due);
        assertThat(progress.getState()).isEqualTo(CardLearningState.NEW);
        assertThat(progress.getStability()).isZero();
        assertThat(progress.getDifficulty()).isZero();
        assertThat(progress.getLastReview()).isNull();
        assertThat(progress.getReps()).isZero();
        assertThat(progress.getLapses()).isZero();
        assertThat(progress.getLearningSteps()).isZero();
        assertThat(progress.getScheduledDays()).isZero();
    }

    @Test
    void givenStudiedProgress_whenResettingToNew_thenAllFieldsGoBackToNewDefaults() {
        FsrsProgress progress = new FsrsProgress(Instant.parse("2026-09-17T12:00:00Z"));
        progress.applySeed(new FsrsProgressSeed(
                CardLearningState.REVIEW,
                4.2,
                5.1,
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-09-17T12:00:00Z"),
                3,
                1,
                0,
                14));

        Instant resetDue = Instant.parse("2026-09-18T04:00:00Z");
        progress.resetToNew(resetDue);

        assertThat(progress.getState()).isEqualTo(CardLearningState.NEW);
        assertThat(progress.getStability()).isZero();
        assertThat(progress.getDifficulty()).isZero();
        assertThat(progress.getDue()).isEqualTo(resetDue);
        assertThat(progress.getLastReview()).isNull();
        assertThat(progress.getReps()).isZero();
        assertThat(progress.getLapses()).isZero();
        assertThat(progress.getLearningSteps()).isZero();
        assertThat(progress.getScheduledDays()).isZero();
    }

    @Test
    void givenSeed_whenApplying_thenAllFieldsMatchSeed() {
        FsrsProgress progress = new FsrsProgress(Instant.parse("2026-09-17T12:00:00Z"));
        FsrsProgressSeed seed = new FsrsProgressSeed(
                CardLearningState.REVIEW,
                4.2,
                5.1,
                Instant.parse("2026-10-01T12:00:00Z"),
                Instant.parse("2026-09-17T12:00:00Z"),
                3,
                1,
                0,
                14);

        progress.applySeed(seed);

        assertThat(progress.getState()).isEqualTo(CardLearningState.REVIEW);
        assertThat(progress.getStability()).isEqualTo(4.2);
        assertThat(progress.getDifficulty()).isEqualTo(5.1);
        assertThat(progress.getDue()).isEqualTo(seed.due());
        assertThat(progress.getLastReview()).isEqualTo(seed.lastReview());
        assertThat(progress.getReps()).isEqualTo(3);
        assertThat(progress.getLapses()).isEqualTo(1);
        assertThat(progress.getScheduledDays()).isEqualTo(14);
    }
}

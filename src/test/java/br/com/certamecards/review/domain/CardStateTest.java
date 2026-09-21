package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardStateTest {

    @Test
    void givenIdAndDue_whenConstructing_thenGettersReturnDefaults() {
        CardStateId id = new CardStateId(UUID.randomUUID(), UUID.randomUUID());
        Instant due = Instant.parse("2026-09-17T12:00:00Z");
        CardState state = new CardState(id, due);
        assertThat(state.getId()).isEqualTo(id);
        assertThat(state.getProgress().getDue()).isEqualTo(due);
        assertThat(state.getReviewCount()).isZero();
        assertThat(state.isSuspended()).isFalse();
        assertThat(state.getUpdatedAt()).isNull();
        assertThat(state.getChangeSeq()).isNull();
    }

    @Test
    void givenCardState_whenSuspending_thenSuspendedAndUpdatedAtChange() {
        CardStateId id = new CardStateId(UUID.randomUUID(), UUID.randomUUID());
        CardState state = new CardState(id, Instant.parse("2026-09-17T12:00:00Z"));
        Instant now = Instant.parse("2026-09-17T13:00:00Z");

        state.setSuspended(true, now);

        assertThat(state.isSuspended()).isTrue();
        assertThat(state.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void givenStudiedCardState_whenResetting_thenProgressIsNewAndReviewCountIncrements() {
        CardStateId id = new CardStateId(UUID.randomUUID(), UUID.randomUUID());
        CardState state = new CardState(id, Instant.parse("2026-09-17T12:00:00Z"));
        state.seedProgress(
                new FsrsProgressSeed(
                        CardLearningState.REVIEW,
                        4.2,
                        5.1,
                        Instant.parse("2026-10-01T12:00:00Z"),
                        Instant.parse("2026-09-17T12:00:00Z"),
                        3,
                        1,
                        0,
                        14),
                3,
                Instant.parse("2026-09-17T12:00:00Z"));
        Instant now = Instant.parse("2026-09-18T04:00:00Z");

        state.reset(now);

        assertThat(state.getProgress().getState()).isEqualTo(CardLearningState.NEW);
        assertThat(state.getReviewCount()).isEqualTo(4);
        assertThat(state.getUpdatedAt()).isEqualTo(now);
    }
}

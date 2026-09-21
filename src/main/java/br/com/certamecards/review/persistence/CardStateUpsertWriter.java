package br.com.certamecards.review.persistence;

import br.com.certamecards.review.domain.CardStateSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CardStateUpsertWriter {

    private static final String SQL =
            """
            INSERT INTO card_states
                (user_id, card_id, state, stability, difficulty, due, last_review,
                 reps, lapses, learning_steps, scheduled_days, review_count, suspended, updated_at)
            VALUES
                (:userId, :cardId, :state, :stability, :difficulty, :due, :lastReview,
                 :reps, :lapses, :learningSteps, :scheduledDays, :reviewCount, false, :updatedAt)
            ON CONFLICT (user_id, card_id) DO UPDATE SET
                state = excluded.state, stability = excluded.stability, difficulty = excluded.difficulty,
                due = excluded.due, last_review = excluded.last_review, reps = excluded.reps,
                lapses = excluded.lapses, learning_steps = excluded.learning_steps,
                scheduled_days = excluded.scheduled_days, review_count = excluded.review_count,
                updated_at = excluded.updated_at
            RETURNING change_seq
            """;

    private final JdbcClient jdbcClient;

    public CardStateUpsertWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long upsert(UUID userId, UUID cardId, CardStateSnapshot snapshot, int reviewCount, Instant now) {
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cardId", cardId)
                .param("state", snapshot.state())
                .param("stability", snapshot.stability())
                .param("difficulty", snapshot.difficulty())
                .param("due", Timestamp.from(snapshot.due()))
                .param("lastReview", timestampOrNull(snapshot.lastReview()))
                .param("reps", snapshot.reps())
                .param("lapses", snapshot.lapses())
                .param("learningSteps", snapshot.learningSteps())
                .param("scheduledDays", snapshot.scheduledDays())
                .param("reviewCount", reviewCount)
                .param("updatedAt", Timestamp.from(now))
                .query(Long.class)
                .single();
    }

    private Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}

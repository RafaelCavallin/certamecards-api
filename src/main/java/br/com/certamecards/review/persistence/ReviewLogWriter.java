package br.com.certamecards.review.persistence;

import br.com.certamecards.review.service.ReviewLogInput;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReviewLogWriter {

    private static final String SQL =
            """
            INSERT INTO review_logs
                (id, user_id, card_id, kind, rating, reviewed_at, duration_ms,
                 state_before, state_after, offline, device_id, session_id, received_at)
            VALUES
                (:id, :userId, :cardId, :kind, :rating, :reviewedAt, :durationMs,
                 CAST(:stateBefore AS jsonb), CAST(:stateAfter AS jsonb), :offline, :deviceId, :sessionId, :receivedAt)
            ON CONFLICT (id) DO NOTHING
            """;

    private final JdbcClient jdbcClient;

    public ReviewLogWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insertAll(UUID userId, List<ReviewLogInput> reviews, Instant receivedAt) {
        reviews.forEach(review -> insertOne(userId, review, receivedAt));
    }

    private void insertOne(UUID userId, ReviewLogInput review, Instant receivedAt) {
        jdbcClient
                .sql(SQL)
                .param("id", review.id())
                .param("userId", userId)
                .param("cardId", review.cardId())
                .param("kind", review.kind())
                .param("rating", review.rating())
                .param("reviewedAt", Timestamp.from(review.reviewedAt()))
                .param("durationMs", review.durationMs())
                .param("stateBefore", review.stateBefore())
                .param("stateAfter", review.stateAfter())
                .param("offline", review.offline())
                .param("deviceId", review.deviceId())
                .param("sessionId", review.sessionId())
                .param("receivedAt", Timestamp.from(receivedAt))
                .update();
    }
}

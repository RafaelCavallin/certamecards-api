package br.com.certamecards.review.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.review.domain.ReviewLogEntry;
import br.com.certamecards.review.domain.ReviewSyncLimits;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class ReviewLogHistoryQuery {

    private static final String SQL =
            """
            SELECT id, card_id, kind, rating, reviewed_at, duration_ms,
                   state_before, state_after, offline, device_id, session_id, change_seq
            FROM review_logs
            WHERE card_id = :cardId AND user_id = :userId
            ORDER BY reviewed_at, id
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public ReviewLogHistoryQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ReviewLogEntry> fetch(UUID userId, UUID cardId) {
        return jdbcClient
                .sql(SQL)
                .param("cardId", cardId)
                .param("userId", userId)
                .param("limit", ReviewSyncLimits.MAX_HISTORY_RECORDS)
                .query((rs, rowNum) -> new ReviewLogEntry(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("card_id"),
                        rs.getString("kind"),
                        ratingOf(rs.getObject("rating")),
                        instant(rs, "reviewed_at"),
                        rs.getInt("duration_ms"),
                        rawValueOf(rs.getString("state_before")),
                        rawValueOf(rs.getString("state_after")),
                        rs.getBoolean("offline"),
                        (UUID) rs.getObject("device_id"),
                        (UUID) rs.getObject("session_id"),
                        rs.getLong("change_seq")))
                .list();
    }

    private RawValue rawValueOf(String json) {
        return json == null ? null : new RawValue(json);
    }

    private Short ratingOf(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }
}

package br.com.certamecards.review.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.review.domain.ReviewHistoryCursor;
import br.com.certamecards.review.domain.ReviewLogEntry;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class ReviewLogHistoryQuery {

    private static final String BASE_SQL =
            """
            SELECT id, card_id, kind, rating, reviewed_at, duration_ms,
                   state_before, state_after, offline, device_id, session_id, change_seq,
                   event_at, event_counter, event_device_id, operation_id
            FROM review_logs
            WHERE card_id = :cardId AND user_id = :userId
            """;

    private static final String CURSOR_CLAUSE =
            """
             AND (event_at, event_counter, event_device_id, operation_id) >
                 (:cursorEventAt, :cursorEventCounter, :cursorEventDeviceId, :cursorOperationId)
            """;

    private static final String ORDER_AND_LIMIT =
            " ORDER BY event_at, event_counter, event_device_id, operation_id LIMIT :limit";

    private final JdbcClient jdbcClient;

    public ReviewLogHistoryQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ReviewLogEntry> fetch(UUID userId, UUID cardId, ReviewHistoryCursor cursor, int limit) {
        String sql = BASE_SQL + (cursor == null ? "" : CURSOR_CLAUSE) + ORDER_AND_LIMIT;
        var spec = jdbcClient
                .sql(sql)
                .param("cardId", cardId)
                .param("userId", userId)
                .param("limit", limit);
        spec = withCursor(spec, cursor);
        return spec.query((rs, rowNum) -> new ReviewLogEntry(
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
                        rs.getLong("change_seq"),
                        instant(rs, "event_at"),
                        rs.getInt("event_counter"),
                        (UUID) rs.getObject("event_device_id"),
                        (UUID) rs.getObject("operation_id")))
                .list();
    }

    private JdbcClient.StatementSpec withCursor(JdbcClient.StatementSpec spec, ReviewHistoryCursor cursor) {
        if (cursor == null) {
            return spec;
        }
        return spec.param("cursorEventAt", Timestamp.from(cursor.eventAt()))
                .param("cursorEventCounter", cursor.eventCounter())
                .param("cursorEventDeviceId", cursor.eventDeviceId())
                .param("cursorOperationId", cursor.operationId());
    }

    private RawValue rawValueOf(String json) {
        return json == null ? null : new RawValue(json);
    }

    private Short ratingOf(Object value) {
        return value == null ? null : ((Number) value).shortValue();
    }
}

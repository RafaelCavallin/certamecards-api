package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.ReviewLogChange;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class ReviewLogsChangesQuery {

    private static final String BASE_SQL =
            """
            SELECT rl.id, rl.card_id, rl.kind, rl.rating, rl.reviewed_at, rl.duration_ms,
                   rl.state_before, rl.state_after, rl.offline, rl.device_id, rl.session_id, rl.change_seq
            FROM review_logs rl
            WHERE rl.user_id = :userId AND rl.change_seq > :cursor
              AND rl.write_xid < pg_snapshot_xmin(pg_current_snapshot())
            """;

    private static final String WINDOW_CLAUSE = " AND rl.reviewed_at > :windowStart ";
    private static final String ORDER_AND_LIMIT = " ORDER BY rl.change_seq LIMIT :limit";

    private final JdbcClient jdbcClient;

    public ReviewLogsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ReviewLogChange> fetch(UUID userId, long cursor, int limit, Instant windowStart) {
        if (limit <= 0) {
            return List.of();
        }
        boolean applyWindow = cursor == 0;
        String sql = BASE_SQL + (applyWindow ? WINDOW_CLAUSE : "") + ORDER_AND_LIMIT;
        var spec = jdbcClient
                .sql(sql)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit);
        if (applyWindow) {
            spec = spec.param("windowStart", Timestamp.from(windowStart));
        }
        return spec.query((rs, rowNum) -> new ReviewLogChange(
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

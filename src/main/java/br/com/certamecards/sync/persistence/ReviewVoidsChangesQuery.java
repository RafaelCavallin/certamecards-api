package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.ReviewVoidChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReviewVoidsChangesQuery {

    private static final String SQL =
            """
            SELECT rv.review_id, rv.voided_at, rv.change_seq
            FROM review_voids rv
            JOIN review_logs rl ON rl.id = rv.review_id
            WHERE rl.user_id = :userId AND rv.change_seq > :cursor
              AND rv.write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY rv.change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public ReviewVoidsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ReviewVoidChange> fetch(UUID userId, long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new ReviewVoidChange(
                        (UUID) rs.getObject("review_id"), instant(rs, "voided_at"), rs.getLong("change_seq")))
                .list();
    }
}

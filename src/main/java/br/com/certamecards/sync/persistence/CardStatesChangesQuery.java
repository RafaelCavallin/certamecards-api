package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.CardStateChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CardStatesChangesQuery {

    private static final String SQL =
            """
            SELECT card_id, state, stability, difficulty, due, last_review, reps, lapses,
                   learning_steps, scheduled_days, review_count, suspended,
                   content_update_note, content_updated_at, change_seq
            FROM card_states
            WHERE user_id = :userId AND change_seq > :cursor
              AND write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public CardStatesChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<CardStateChange> fetch(UUID userId, long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new CardStateChange(
                        (UUID) rs.getObject("card_id"),
                        rs.getInt("state"),
                        rs.getDouble("stability"),
                        rs.getDouble("difficulty"),
                        instant(rs, "due"),
                        instant(rs, "last_review"),
                        rs.getInt("reps"),
                        rs.getInt("lapses"),
                        rs.getInt("learning_steps"),
                        rs.getInt("scheduled_days"),
                        rs.getInt("review_count"),
                        rs.getBoolean("suspended"),
                        rs.getString("content_update_note"),
                        instant(rs, "content_updated_at"),
                        rs.getLong("change_seq")))
                .list();
    }
}

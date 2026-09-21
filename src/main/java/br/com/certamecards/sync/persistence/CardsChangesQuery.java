package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.CardChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CardsChangesQuery {

    private static final String SQL =
            """
            SELECT c.id, c.deck_id, c.type, c.front, c.back, c.source,
                   c.created_at, c.updated_at, c.deleted_at, c.version, c.change_seq
            FROM cards c JOIN decks d ON d.id = c.deck_id
            WHERE (d.owner_id = :userId OR EXISTS (
                       SELECT 1 FROM deck_subscriptions s
                       WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL))
              AND c.change_seq > :cursor
              AND c.write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY c.change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public CardsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<CardChange> fetch(UUID userId, long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new CardChange(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("deck_id"),
                        rs.getString("type"),
                        rs.getString("front"),
                        rs.getString("back"),
                        rs.getString("source"),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at"),
                        instant(rs, "deleted_at"),
                        rs.getInt("version"),
                        rs.getLong("change_seq")))
                .list();
    }
}

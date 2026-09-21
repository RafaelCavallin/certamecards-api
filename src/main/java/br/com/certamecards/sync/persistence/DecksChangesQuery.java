package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.DeckChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DecksChangesQuery {

    private static final String SQL =
            """
            SELECT d.id, d.subject_id, d.name, d.description, d.origin, d.origin_ref,
                   d.official_status, d.origin_label, d.content_updated_at, d.card_count,
                   d.created_at, d.updated_at, d.deleted_at, d.version, d.change_seq
            FROM decks d
            WHERE (d.owner_id = :userId OR EXISTS (
                       SELECT 1 FROM deck_subscriptions s
                       WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL))
              AND d.change_seq > :cursor
              AND d.write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY d.change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public DecksChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<DeckChange> fetch(UUID userId, long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new DeckChange(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("subject_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("origin"),
                        (UUID) rs.getObject("origin_ref"),
                        rs.getString("official_status"),
                        rs.getString("origin_label"),
                        instant(rs, "content_updated_at"),
                        rs.getInt("card_count"),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at"),
                        instant(rs, "deleted_at"),
                        rs.getInt("version"),
                        rs.getLong("change_seq")))
                .list();
    }
}

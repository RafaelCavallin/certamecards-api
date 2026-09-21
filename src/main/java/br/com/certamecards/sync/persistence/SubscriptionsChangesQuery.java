package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.DeckSubscriptionChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionsChangesQuery {

    private static final String SQL =
            """
            SELECT deck_id, subscribed_at, cancelled_at, change_seq
            FROM deck_subscriptions
            WHERE user_id = :userId AND change_seq > :cursor
              AND write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public SubscriptionsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<DeckSubscriptionChange> fetch(UUID userId, long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new DeckSubscriptionChange(
                        (UUID) rs.getObject("deck_id"),
                        instant(rs, "subscribed_at"),
                        instant(rs, "cancelled_at"),
                        rs.getLong("change_seq")))
                .list();
    }
}

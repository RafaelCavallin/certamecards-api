package br.com.certamecards.officialdeck.persistence;

import br.com.certamecards.officialdeck.domain.PurgeTarget;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PurgeableSubscriptionsQuery {

    private static final String SQL =
            """
            SELECT user_id, deck_id FROM deck_subscriptions
            WHERE cancelled_at IS NOT NULL AND cancelled_at < :threshold AND progress_purged_at IS NULL
            ORDER BY cancelled_at, user_id LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public PurgeableSubscriptionsQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<PurgeTarget> find(Instant threshold, int limit) {
        return jdbcClient
                .sql(SQL)
                .param("threshold", Timestamp.from(threshold))
                .param("limit", limit)
                .query((rs, rowNum) -> new PurgeTarget((UUID) rs.getObject("user_id"), (UUID) rs.getObject("deck_id")))
                .list();
    }
}

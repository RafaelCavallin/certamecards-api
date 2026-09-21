package br.com.certamecards.officialdeck.persistence;

import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateSubscribersQuery {

    private static final String SQL =
            """
            SELECT user_id FROM deck_subscriptions
            WHERE deck_id = :deckId AND cancelled_at IS NULL AND subscribed_at <= :updatedAt
              AND (CAST(:cursor AS uuid) IS NULL OR user_id > :cursor)
            ORDER BY user_id LIMIT :limit FOR UPDATE SKIP LOCKED
            """;

    private final JdbcClient jdbcClient;

    public ContentUpdateSubscribersQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<UUID> lockNextBatch(ContentUpdateJob job, int limit) {
        return jdbcClient
                .sql(SQL)
                .param("deckId", job.deckId())
                .param("updatedAt", Timestamp.from(job.updatedAt()))
                .param("cursor", job.cursorUserId(), Types.OTHER)
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }
}

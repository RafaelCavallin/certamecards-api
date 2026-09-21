package br.com.certamecards.sync.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PurgeQuery {

    private static final String MAX_PURGEABLE_CHANGE_SEQ =
            """
            SELECT COALESCE(MAX(change_seq), 0) FROM (
                SELECT change_seq FROM cards c WHERE c.deleted_at IS NOT NULL AND c.deleted_at < :threshold
                    AND NOT EXISTS (SELECT 1 FROM card_states cs WHERE cs.card_id = c.id)
                    AND NOT EXISTS (SELECT 1 FROM review_logs rl WHERE rl.card_id = c.id)
                UNION ALL
                SELECT change_seq FROM decks d WHERE d.deleted_at IS NOT NULL AND d.deleted_at < :threshold
                    AND NOT EXISTS (SELECT 1 FROM cards c2 WHERE c2.deck_id = d.id)
            ) purgeable
            """;

    private static final String DELETE_PURGEABLE_CARDS =
            """
            DELETE FROM cards c WHERE c.deleted_at IS NOT NULL AND c.deleted_at < :threshold
                AND NOT EXISTS (SELECT 1 FROM card_states cs WHERE cs.card_id = c.id)
                AND NOT EXISTS (SELECT 1 FROM review_logs rl WHERE rl.card_id = c.id)
            """;

    private static final String DELETE_PURGEABLE_DECKS =
            """
            DELETE FROM decks d WHERE d.deleted_at IS NOT NULL AND d.deleted_at < :threshold
                AND NOT EXISTS (SELECT 1 FROM cards c2 WHERE c2.deck_id = d.id)
            """;

    private final JdbcClient jdbcClient;

    public PurgeQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long maxPurgeableChangeSeq(Instant threshold) {
        return jdbcClient
                .sql(MAX_PURGEABLE_CHANGE_SEQ)
                .param("threshold", Timestamp.from(threshold))
                .query(Long.class)
                .single();
    }

    public int purgeCards(Instant threshold) {
        return jdbcClient
                .sql(DELETE_PURGEABLE_CARDS)
                .param("threshold", Timestamp.from(threshold))
                .update();
    }

    public int purgeDecks(Instant threshold) {
        return jdbcClient
                .sql(DELETE_PURGEABLE_DECKS)
                .param("threshold", Timestamp.from(threshold))
                .update();
    }
}

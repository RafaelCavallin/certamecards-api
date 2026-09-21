package br.com.certamecards.library.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class UserCardCountsQuery {

    private static final String USED_CARDS_SQL =
            """
            SELECT (SELECT count(*) FROM cards c JOIN decks d ON d.id = c.deck_id
                    WHERE d.owner_id = :userId AND c.deleted_at IS NULL)
                 + (SELECT COALESCE(sum(d.card_count), 0) FROM deck_subscriptions s
                    JOIN decks d ON d.id = s.deck_id
                    WHERE s.user_id = :userId AND s.cancelled_at IS NULL)
            """;

    private final JdbcClient jdbcClient;

    public UserCardCountsQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public long usedCards(UUID userId) {
        return jdbcClient
                .sql(USED_CARDS_SQL)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}

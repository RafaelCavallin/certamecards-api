package br.com.certamecards.library.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.library.domain.DeckSubscription;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DeckSubscriptionStore {

    private static final String COLUMNS = "deck_id, subscribed_at, cancelled_at, change_seq";
    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM deck_subscriptions WHERE user_id = :userId AND deck_id = :deckId";
    private static final String UPSERT_SQL =
            """
            INSERT INTO deck_subscriptions (user_id, deck_id, subscribed_at)
            VALUES (:userId, :deckId, :now)
            ON CONFLICT (user_id, deck_id) DO UPDATE
            SET subscribed_at = :now, cancelled_at = NULL, progress_purged_at = NULL
            """
                    + " RETURNING "
                    + COLUMNS;
    private static final String CANCEL_SQL =
            """
            UPDATE deck_subscriptions SET cancelled_at = :now
            WHERE user_id = :userId AND deck_id = :deckId AND cancelled_at IS NULL
            """;
    private static final String STORED_PROGRESS_SQL =
            """
            SELECT EXISTS (SELECT 1 FROM card_states cs JOIN cards c ON c.id = cs.card_id
                           WHERE cs.user_id = :userId AND c.deck_id = :deckId
                             AND (cs.state <> 0 OR cs.suspended))
            """;

    private final JdbcClient jdbcClient;

    public DeckSubscriptionStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<DeckSubscription> find(UUID userId, UUID deckId) {
        return jdbcClient
                .sql(FIND_SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .query((rs, rowNum) -> toSubscription(rs))
                .optional();
    }

    public DeckSubscription activate(UUID userId, UUID deckId, Instant now) {
        return jdbcClient
                .sql(UPSERT_SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("now", Timestamp.from(now))
                .query((rs, rowNum) -> toSubscription(rs))
                .single();
    }

    public void cancel(UUID userId, UUID deckId, Instant now) {
        jdbcClient
                .sql(CANCEL_SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("now", Timestamp.from(now))
                .update();
    }

    public boolean hasStoredProgress(UUID userId, UUID deckId) {
        return jdbcClient
                .sql(STORED_PROGRESS_SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .query(Boolean.class)
                .single();
    }

    private DeckSubscription toSubscription(ResultSet rs) throws SQLException {
        return new DeckSubscription(
                (UUID) rs.getObject("deck_id"),
                instant(rs, "subscribed_at"),
                instant(rs, "cancelled_at"),
                rs.getLong("change_seq"));
    }
}

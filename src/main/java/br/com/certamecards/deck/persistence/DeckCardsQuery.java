package br.com.certamecards.deck.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DeckCardsQuery {

    private final JdbcClient jdbcClient;

    public DeckCardsQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<UUID> activeCardIds(UUID deckId) {
        return jdbcClient
                .sql("SELECT id FROM cards WHERE deck_id = :deckId AND deleted_at IS NULL")
                .param("deckId", deckId)
                .query(UUID.class)
                .list();
    }

    public void markAllDeleted(UUID deckId, Instant now) {
        jdbcClient
                .sql("UPDATE cards SET deleted_at = :now WHERE deck_id = :deckId AND deleted_at IS NULL")
                .param("now", Timestamp.from(now))
                .param("deckId", deckId)
                .update();
    }
}

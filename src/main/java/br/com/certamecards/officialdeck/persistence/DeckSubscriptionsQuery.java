package br.com.certamecards.officialdeck.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DeckSubscriptionsQuery {

    private final JdbcClient jdbcClient;

    public DeckSubscriptionsQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean everHadSubscribers(UUID deckId) {
        return Boolean.TRUE.equals(jdbcClient
                .sql("SELECT EXISTS(SELECT 1 FROM deck_subscriptions WHERE deck_id = :deckId)")
                .param("deckId", deckId)
                .query(Boolean.class)
                .single());
    }
}

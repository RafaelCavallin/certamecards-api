package br.com.certamecards.library.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SubscriberCountUpdater {

    private static final String ADJUST_SQL =
            "UPDATE decks SET subscriber_count = subscriber_count + :delta WHERE id = :deckId";

    private final JdbcClient jdbcClient;

    public SubscriberCountUpdater(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void increment(UUID deckId) {
        adjust(deckId, 1);
    }

    public void decrement(UUID deckId) {
        adjust(deckId, -1);
    }

    private void adjust(UUID deckId, int delta) {
        jdbcClient.sql(ADJUST_SQL).param("delta", delta).param("deckId", deckId).update();
    }
}

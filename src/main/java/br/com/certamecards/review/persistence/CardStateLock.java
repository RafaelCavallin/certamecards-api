package br.com.certamecards.review.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CardStateLock {

    private static final String SQL = "SELECT id FROM cards WHERE id IN (:cardIds) ORDER BY id FOR UPDATE";

    private final JdbcClient jdbcClient;

    public CardStateLock(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void lockAll(List<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return;
        }
        jdbcClient.sql(SQL).param("cardIds", cardIds).query(UUID.class).list();
    }
}

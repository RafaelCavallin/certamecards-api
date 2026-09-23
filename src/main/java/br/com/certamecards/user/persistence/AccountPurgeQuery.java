package br.com.certamecards.user.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AccountPurgeQuery {

    private final JdbcClient jdbcClient;

    public AccountPurgeQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void purge(UUID userId) {
        execute("DELETE FROM refresh_tokens WHERE user_id = :userId", userId);
        execute("DELETE FROM one_time_tokens WHERE user_id = :userId", userId);
        execute("DELETE FROM oauth_identities WHERE user_id = :userId", userId);
        execute(
                "DELETE FROM review_voids WHERE review_id IN "
                        + "(SELECT rl.id FROM review_logs rl JOIN cards c ON c.id = rl.card_id "
                        + "JOIN decks d ON d.id = c.deck_id WHERE d.owner_id = :userId)",
                userId);
        execute(
                "DELETE FROM review_logs WHERE card_id IN "
                        + "(SELECT c.id FROM cards c JOIN decks d ON d.id = c.deck_id WHERE d.owner_id = :userId)",
                userId);
        execute("DELETE FROM card_states WHERE user_id = :userId", userId);
        execute("DELETE FROM cards WHERE deck_id IN (SELECT id FROM decks WHERE owner_id = :userId)", userId);
        execute("DELETE FROM decks WHERE owner_id = :userId", userId);
        execute("UPDATE card_error_reports SET closed_by = NULL WHERE closed_by = :userId", userId);
        execute("DELETE FROM card_error_reports WHERE user_id = :userId", userId);
        execute("DELETE FROM user_settings WHERE user_id = :userId", userId);
        execute("UPDATE product_events SET user_id = NULL WHERE user_id = :userId", userId);
        execute("DELETE FROM users WHERE id = :userId", userId);
    }

    private void execute(String sql, UUID userId) {
        jdbcClient.sql(sql).param("userId", userId).update();
    }
}

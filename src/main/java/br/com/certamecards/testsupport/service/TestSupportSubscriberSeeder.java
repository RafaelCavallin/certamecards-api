package br.com.certamecards.testsupport.service;

import br.com.certamecards.library.service.SubscriptionService;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class TestSupportSubscriberSeeder {

    private static final int SUBSCRIBERS_PER_DECK = 2;
    private static final String INSERT_USER_SQL =
            "INSERT INTO users (id, email, display_name, role, email_verified_at) "
                    + "VALUES (:id, :email, 'Inscrito de teste', 'candidate', now())";

    private final JdbcClient jdbcClient;
    private final SubscriptionService subscriptionService;

    public TestSupportSubscriberSeeder(JdbcClient jdbcClient, SubscriptionService subscriptionService) {
        this.jdbcClient = jdbcClient;
        this.subscriptionService = subscriptionService;
    }

    public void seed(UUID deckId) {
        for (int index = 0; index < SUBSCRIBERS_PER_DECK; index++) {
            UUID userId = UUID.randomUUID();
            jdbcClient
                    .sql(INSERT_USER_SQL)
                    .param("id", userId)
                    .param("email", "inscrito-" + userId + "@exemplo.com")
                    .update();
            subscriptionService.subscribe(userId, deckId);
        }
    }
}

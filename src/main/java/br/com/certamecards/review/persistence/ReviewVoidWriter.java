package br.com.certamecards.review.persistence;

import br.com.certamecards.review.service.ReviewVoidInput;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReviewVoidWriter {

    private static final String OWNED_IDS_SQL =
            """
            SELECT rl.id FROM review_logs rl
            JOIN cards c ON c.id = rl.card_id
            JOIN decks d ON d.id = c.deck_id
            WHERE d.owner_id = :ownerId AND rl.id IN (:ids)
            """;

    private static final String INSERT_SQL =
            """
            INSERT INTO review_voids (review_id, voided_at)
            VALUES (:reviewId, :voidedAt)
            ON CONFLICT (review_id) DO NOTHING
            """;

    private final JdbcClient jdbcClient;

    public ReviewVoidWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<UUID> insertAll(UUID ownerId, List<ReviewVoidInput> voids) {
        if (voids.isEmpty()) {
            return List.of();
        }
        Set<UUID> owned = ownedReviewIds(ownerId, voids);
        List<UUID> accepted = new ArrayList<>();
        voids.forEach(voidInput -> insertIfOwned(voidInput, owned, accepted));
        return accepted;
    }

    private void insertIfOwned(ReviewVoidInput voidInput, Set<UUID> owned, List<UUID> accepted) {
        if (!owned.contains(voidInput.reviewId())) {
            return;
        }
        jdbcClient
                .sql(INSERT_SQL)
                .param("reviewId", voidInput.reviewId())
                .param("voidedAt", Timestamp.from(voidInput.voidedAt()))
                .update();
        accepted.add(voidInput.reviewId());
    }

    private Set<UUID> ownedReviewIds(UUID ownerId, List<ReviewVoidInput> voids) {
        List<UUID> ids = voids.stream().map(ReviewVoidInput::reviewId).toList();
        return Set.copyOf(jdbcClient
                .sql(OWNED_IDS_SQL)
                .param("ownerId", ownerId)
                .param("ids", ids)
                .query(UUID.class)
                .list());
    }
}

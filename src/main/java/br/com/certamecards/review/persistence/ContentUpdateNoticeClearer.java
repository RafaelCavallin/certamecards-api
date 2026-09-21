package br.com.certamecards.review.persistence;

import br.com.certamecards.review.service.ReviewLogInput;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateNoticeClearer {

    private static final String REVIEW_KIND = "review";
    private static final String SQL =
            """
            UPDATE card_states SET content_update_note = NULL, content_updated_at = NULL
            WHERE user_id = :userId AND card_id = :cardId
              AND content_updated_at IS NOT NULL AND content_updated_at <= :reviewedAt
            """;

    private final JdbcClient jdbcClient;

    public ContentUpdateNoticeClearer(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void clearFor(UUID userId, List<ReviewLogInput> acceptedReviews) {
        acceptedReviews.stream()
                .filter(review -> REVIEW_KIND.equals(review.kind()))
                .forEach(review -> clearOne(userId, review));
    }

    private void clearOne(UUID userId, ReviewLogInput review) {
        jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cardId", review.cardId())
                .param("reviewedAt", Timestamp.from(review.reviewedAt()))
                .update();
    }
}

package br.com.certamecards.review.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.review.domain.ReviewSyncLimits;
import br.com.certamecards.review.domain.ReviewVoidEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReviewVoidHistoryQuery {

    private static final String SQL =
            """
            SELECT rv.review_id, rv.voided_at, rv.change_seq
            FROM review_voids rv
            JOIN review_logs rl ON rl.id = rv.review_id
            WHERE rl.card_id = :cardId AND rl.user_id = :userId
            ORDER BY rv.voided_at, rv.review_id
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public ReviewVoidHistoryQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ReviewVoidEntry> fetch(UUID userId, UUID cardId) {
        return jdbcClient
                .sql(SQL)
                .param("cardId", cardId)
                .param("userId", userId)
                .param("limit", ReviewSyncLimits.MAX_HISTORY_RECORDS)
                .query((rs, rowNum) -> new ReviewVoidEntry(
                        (UUID) rs.getObject("review_id"), instant(rs, "voided_at"), rs.getLong("change_seq")))
                .list();
    }
}

package br.com.certamecards.errorreport.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReportableCardQuery {

    private static final String EXISTS_SQL =
            """
            SELECT EXISTS (
                SELECT 1 FROM cards c JOIN decks d ON d.id = c.deck_id
                WHERE c.id = :cardId AND c.deleted_at IS NULL AND d.deleted_at IS NULL AND d.owner_id IS NULL
                  AND (d.official_status = 'published' OR EXISTS (
                      SELECT 1 FROM deck_subscriptions s
                      WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL)))
            """;

    private final JdbcClient jdbcClient;

    public ReportableCardQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean isReportable(UUID userId, UUID cardId) {
        Boolean reportable = jdbcClient
                .sql(EXISTS_SQL)
                .param("cardId", cardId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
        return reportable;
    }
}

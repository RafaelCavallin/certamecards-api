package br.com.certamecards.library.persistence;

import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.PreviewCard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LibraryDeckLookupQuery {

    private static final String BY_ID_SQL = LibrarySql.SUMMARY_SELECT + "WHERE d.id = :deckId";
    private static final String SUGGESTION_SQL = LibrarySql.SUMMARY_SELECT + LibrarySql.PUBLISHED_FILTER
            + "AND NOT EXISTS (SELECT 1 FROM deck_subscriptions own WHERE own.deck_id = d.id "
            + "AND own.user_id = :userId AND own.cancelled_at IS NULL) "
            + "ORDER BY d.card_count DESC, s.normalized_name, d.id LIMIT :pool";
    private static final String PREVIEW_SQL =
            "SELECT id, front, back, source FROM cards WHERE deck_id = :deckId AND deleted_at IS NULL "
                    + "ORDER BY id LIMIT :limit";

    private final JdbcClient jdbcClient;

    public LibraryDeckLookupQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<LibraryDeckSummary> findSummary(UUID userId, UUID deckId) {
        return jdbcClient
                .sql(BY_ID_SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .query(LibraryDeckSummaryRowMapper.INSTANCE)
                .optional();
    }

    public List<LibraryDeckSummary> suggestionCandidates(UUID userId, int pool) {
        return jdbcClient
                .sql(SUGGESTION_SQL)
                .param("userId", userId)
                .param("pool", pool)
                .query(LibraryDeckSummaryRowMapper.INSTANCE)
                .list();
    }

    public List<PreviewCard> previewCards(UUID deckId, int limit) {
        return jdbcClient
                .sql(PREVIEW_SQL)
                .param("deckId", deckId)
                .param("limit", limit)
                .query((rs, rowNum) -> new PreviewCard(
                        (UUID) rs.getObject("id"), rs.getString("front"), rs.getString("back"), rs.getString("source")))
                .list();
    }
}

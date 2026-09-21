package br.com.certamecards.officialdeck.persistence;

import br.com.certamecards.officialdeck.domain.OfficialDeckAdminFilter;
import br.com.certamecards.officialdeck.domain.OfficialDeckAdminSummary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckAdminQuery {

    private static final String SELECT_SQL =
            """
            SELECT d.id, d.subject_id, s.name AS subject_name, d.name, d.description, d.official_status AS status,
                   d.card_count, d.subscriber_count, COALESCE(r.open_count, 0) AS open_report_count,
                   d.content_updated_at, d.version
            FROM decks d
            JOIN subjects s ON s.id = d.subject_id
            LEFT JOIN (
                SELECT c.deck_id, COUNT(*) AS open_count
                FROM card_error_reports er JOIN cards c ON c.id = er.card_id
                WHERE er.status = 'open'
                GROUP BY c.deck_id
            ) r ON r.deck_id = d.id
            WHERE d.owner_id IS NULL
            """;

    private final JdbcClient jdbcClient;

    public OfficialDeckAdminQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<OfficialDeckAdminSummary> search(OfficialDeckAdminFilter filter) {
        StringBuilder sql = new StringBuilder(SELECT_SQL);
        appendFilters(sql, filter);
        sql.append(" ORDER BY d.official_status, s.name, d.name LIMIT :size OFFSET :offset");
        Map<String, Object> params = paramsOf(filter);
        params.put("size", filter.size());
        params.put("offset", filter.page() * filter.size());
        return jdbcClient
                .sql(sql.toString())
                .params(params)
                .query(OfficialDeckAdminSummaryRowMapper::map)
                .list();
    }

    public long count(OfficialDeckAdminFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM decks d WHERE d.owner_id IS NULL");
        appendFilters(sql, filter);
        Long total = jdbcClient
                .sql(sql.toString())
                .params(paramsOf(filter))
                .query(Long.class)
                .single();
        return total;
    }

    public Optional<OfficialDeckAdminSummary> findById(UUID deckId) {
        return jdbcClient
                .sql(SELECT_SQL + " AND d.id = :deckId")
                .param("deckId", deckId)
                .query(OfficialDeckAdminSummaryRowMapper::map)
                .optional();
    }

    private void appendFilters(StringBuilder sql, OfficialDeckAdminFilter filter) {
        if (filter.status() != null) {
            sql.append(" AND d.official_status = :status");
        }
        if (filter.subjectId() != null) {
            sql.append(" AND d.subject_id = :subjectId");
        }
    }

    private Map<String, Object> paramsOf(OfficialDeckAdminFilter filter) {
        Map<String, Object> params = new HashMap<>();
        if (filter.status() != null) {
            params.put("status", filter.status().code());
        }
        if (filter.subjectId() != null) {
            params.put("subjectId", filter.subjectId());
        }
        return params;
    }
}

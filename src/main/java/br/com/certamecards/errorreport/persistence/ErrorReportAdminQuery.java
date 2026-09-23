package br.com.certamecards.errorreport.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.errorreport.domain.ErrorReportFilter;
import br.com.certamecards.errorreport.domain.ErrorReportView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ErrorReportAdminQuery {

    private static final String SELECT_SQL =
            """
            SELECT er.id, er.card_id, d.id AS deck_id, d.name AS deck_name, s.name AS subject_name,
                   c.front, u.display_name, er.reason, er.note, er.status, er.created_at, er.closed_at, er.closed_by
            FROM card_error_reports er
            JOIN cards c ON c.id = er.card_id
            JOIN decks d ON d.id = c.deck_id
            JOIN subjects s ON s.id = d.subject_id
            JOIN users u ON u.id = er.user_id
            """;

    private final JdbcClient jdbcClient;

    public ErrorReportAdminQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ErrorReportView> search(ErrorReportFilter filter) {
        return jdbcClient
                .sql(SELECT_SQL + " WHERE er.status = :status ORDER BY er.created_at, er.id LIMIT :size OFFSET :offset")
                .param("status", filter.status().code())
                .param("size", filter.size())
                .param("offset", filter.page() * filter.size())
                .query(ErrorReportAdminQuery::map)
                .list();
    }

    public long count(ErrorReportFilter filter) {
        Long total = jdbcClient
                .sql("SELECT COUNT(*) FROM card_error_reports WHERE status = :status")
                .param("status", filter.status().code())
                .query(Long.class)
                .single();
        return total;
    }

    public Optional<ErrorReportView> findById(UUID id) {
        return jdbcClient
                .sql(SELECT_SQL + " WHERE er.id = :id")
                .param("id", id)
                .query(ErrorReportAdminQuery::map)
                .optional();
    }

    private static ErrorReportView map(ResultSet rs, int rowNum) throws SQLException {
        return new ErrorReportView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("card_id"),
                (UUID) rs.getObject("deck_id"),
                rs.getString("deck_name"),
                rs.getString("subject_name"),
                rs.getString("front"),
                rs.getString("display_name"),
                rs.getString("reason"),
                rs.getString("note"),
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "closed_at"),
                (UUID) rs.getObject("closed_by"));
    }
}

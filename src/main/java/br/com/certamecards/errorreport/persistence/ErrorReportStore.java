package br.com.certamecards.errorreport.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.errorreport.domain.ErrorReport;
import br.com.certamecards.errorreport.domain.OpenReportRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ErrorReportStore {

    private static final String INSERT_SQL =
            """
            INSERT INTO card_error_reports (id, card_id, user_id, reason, note, status, created_at)
            VALUES (:id, :cardId, :userId, :reason, :note, :status, :createdAt)
            ON CONFLICT ON CONSTRAINT card_error_reports_card_user_unique DO NOTHING
            """;
    private static final String LOCK_OPEN_SQL =
            """
            SELECT er.id, c.front, er.created_at FROM card_error_reports er JOIN cards c ON c.id = er.card_id
            WHERE er.id = :id AND er.status = 'open' FOR UPDATE OF er
            """;
    private static final String CLOSE_SQL =
            "UPDATE card_error_reports SET status = :status, closed_at = :closedAt, closed_by = :closedBy WHERE id = :id";

    private final JdbcClient jdbcClient;

    public ErrorReportStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean insert(ErrorReport report, UUID userId) {
        return jdbcClient
                        .sql(INSERT_SQL)
                        .param("id", report.id())
                        .param("cardId", report.cardId())
                        .param("userId", userId)
                        .param("reason", report.reason().code())
                        .param("note", report.note())
                        .param("status", report.status().code())
                        .param("createdAt", Timestamp.from(report.createdAt()))
                        .update()
                > 0;
    }

    public Optional<OpenReportRecord> lockOpen(UUID id) {
        return jdbcClient
                .sql(LOCK_OPEN_SQL)
                .param("id", id)
                .query((rs, row) -> new OpenReportRecord(
                        (UUID) rs.getObject("id"), rs.getString("front"), instant(rs, "created_at")))
                .optional();
    }

    public void close(UUID id, String status, UUID closedBy, Instant closedAt) {
        jdbcClient
                .sql(CLOSE_SQL)
                .param("id", id)
                .param("status", status)
                .param("closedBy", closedBy)
                .param("closedAt", Timestamp.from(closedAt))
                .update();
    }

    public boolean exists(UUID id) {
        Boolean found = jdbcClient
                .sql("SELECT EXISTS (SELECT 1 FROM card_error_reports WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
        return found;
    }
}

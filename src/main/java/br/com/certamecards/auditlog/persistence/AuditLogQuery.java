package br.com.certamecards.auditlog.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.auditlog.domain.AuditLogEntry;
import br.com.certamecards.auditlog.domain.AuditLogFilter;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class AuditLogQuery {

    private static final String BASE_SQL =
            """
            SELECT a.id, a.actor_id, u.display_name AS actor_name, a.action, a.target_type,
                   a.target_id, a.target_label, a.changes::text AS changes, a.created_at
            FROM admin_audit_logs a JOIN users u ON u.id = a.actor_id
            WHERE 1 = 1
            """;

    private final JdbcClient jdbcClient;

    public AuditLogQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<AuditLogEntry> search(AuditLogFilter filter) {
        StringBuilder sql = new StringBuilder(BASE_SQL);
        appendFilters(sql, filter);
        sql.append(" ORDER BY a.created_at DESC, a.id DESC LIMIT :size");
        return jdbcClient
                .sql(sql.toString())
                .params(paramsOf(filter))
                .query((rs, rowNum) -> new AuditLogEntry(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("actor_id"),
                        rs.getString("actor_name"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        (UUID) rs.getObject("target_id"),
                        rs.getString("target_label"),
                        rs.getString("changes"),
                        instant(rs, "created_at")))
                .list();
    }

    private void appendFilters(StringBuilder sql, AuditLogFilter filter) {
        if (filter.actorId() != null) {
            sql.append(" AND a.actor_id = :actorId");
        }
        if (filter.action() != null) {
            sql.append(" AND a.action = :action");
        }
        if (filter.from() != null) {
            sql.append(" AND a.created_at >= :from");
        }
        if (filter.to() != null) {
            sql.append(" AND a.created_at < :to");
        }
        if (filter.before() != null) {
            sql.append(" AND (a.created_at, a.id) < (:beforeCreatedAt, :beforeId)");
        }
    }

    private Map<String, Object> paramsOf(AuditLogFilter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("size", filter.size());
        if (filter.actorId() != null) {
            params.put("actorId", filter.actorId());
        }
        if (filter.action() != null) {
            params.put("action", filter.action().code());
        }
        if (filter.from() != null) {
            params.put(
                    "from",
                    Timestamp.from(filter.from().atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (filter.to() != null) {
            params.put(
                    "to",
                    Timestamp.from(
                            filter.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (filter.before() != null) {
            params.put("beforeCreatedAt", Timestamp.from(filter.before().createdAt()));
            params.put("beforeId", filter.before().id());
        }
        return params;
    }
}

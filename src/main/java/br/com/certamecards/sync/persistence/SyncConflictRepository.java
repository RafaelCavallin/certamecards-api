package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.sync.domain.ConflictCursor;
import br.com.certamecards.sync.domain.SyncConflict;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class SyncConflictRepository {

    private static final String COLUMNS =
            """
            id, user_id, entity_type, entity_id, deck_id, losing_operation_id, winning_operation_id, reason,
            losing_snapshot::text AS losing_snapshot, winning_snapshot::text AS winning_snapshot,
            expires_at, restored_at, expired_at, created_at
            """;
    private static final String FIND_SQL =
            "SELECT " + COLUMNS + " FROM sync_conflicts WHERE user_id = :userId AND id = :id";
    private static final String LIST_SQL = "SELECT "
            + COLUMNS
            + """
             FROM sync_conflicts WHERE user_id = :userId
              AND (:hasCursor = false OR (expires_at, id) > (:cursorExpiresAt, :cursorId))
            ORDER BY expires_at, id LIMIT :limit
            """;
    private static final String MARK_RESTORED_SQL =
            "UPDATE sync_conflicts SET restored_at = :restoredAt WHERE id = :id AND restored_at IS NULL";

    private final JdbcClient jdbcClient;

    public SyncConflictRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<SyncConflict> find(UUID userId, UUID id) {
        return jdbcClient
                .sql(FIND_SQL)
                .param("userId", userId)
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public List<SyncConflict> list(UUID userId, ConflictCursor cursor, int limit) {
        return jdbcClient
                .sql(LIST_SQL)
                .param("userId", userId)
                .param("hasCursor", cursor != null)
                .param("cursorExpiresAt", Timestamp.from(cursor == null ? Instant.EPOCH : cursor.expiresAt()))
                .param("cursorId", cursor == null ? new UUID(0L, 0L) : cursor.id())
                .param("limit", limit)
                .query(this::map)
                .list();
    }

    public void markRestored(UUID id, Instant restoredAt) {
        jdbcClient
                .sql(MARK_RESTORED_SQL)
                .param("id", id)
                .param("restoredAt", Timestamp.from(restoredAt))
                .update();
    }

    private SyncConflict map(ResultSet rs, int rowNum) throws SQLException {
        return new SyncConflict(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("user_id"),
                rs.getString("entity_type"),
                (UUID) rs.getObject("entity_id"),
                (UUID) rs.getObject("deck_id"),
                (UUID) rs.getObject("losing_operation_id"),
                (UUID) rs.getObject("winning_operation_id"),
                rs.getString("reason"),
                rawValueOf(rs.getString("losing_snapshot")),
                rawValueOf(rs.getString("winning_snapshot")),
                instant(rs, "expires_at"),
                instant(rs, "restored_at"),
                instant(rs, "expired_at"),
                instant(rs, "created_at"));
    }

    private RawValue rawValueOf(String json) {
        return json == null ? null : new RawValue(json);
    }
}

package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.SyncEntityHead;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SyncEntityHeadRepository {

    private static final String INSERT =
            """
            INSERT INTO sync_entity_heads (user_id, entity_type, entity_id, parent_id)
            VALUES (:userId, :entityType, :entityId, :parentId)
            ON CONFLICT (user_id, entity_type, entity_id) DO NOTHING
            """;
    private static final String LOCK =
            """
            SELECT user_id, entity_type, entity_id, parent_id, version, winning_operation_id, event_at,
                   event_counter, event_device_id, deleted
            FROM sync_entity_heads WHERE user_id = :userId AND entity_type = :entityType AND entity_id = :entityId
            FOR UPDATE
            """;
    private static final String UPDATE =
            """
            UPDATE sync_entity_heads SET parent_id = :parentId, version = :version,
                winning_operation_id = :operationId, event_at = :eventAt, event_counter = :eventCounter,
                event_device_id = :deviceId, deleted = :deleted, updated_at = now()
            WHERE user_id = :userId AND entity_type = :entityType AND entity_id = :entityId
            """;
    private static final String DELETE_CHILDREN =
            """
            UPDATE sync_entity_heads SET winning_operation_id = :operationId, event_at = :eventAt,
                event_counter = :eventCounter, event_device_id = :deviceId, deleted = true, updated_at = now()
            WHERE user_id = :userId AND entity_type = 'card' AND parent_id = :deckId
            """;
    private final JdbcClient jdbcClient;

    public SyncEntityHeadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SyncEntityHead lock(UUID userId, String entityType, UUID entityId, UUID parentId) {
        jdbcClient
                .sql(INSERT)
                .param("userId", userId)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .param("parentId", parentId)
                .update();
        return jdbcClient
                .sql(LOCK)
                .param("userId", userId)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .query(this::map)
                .single();
    }

    public void save(SyncEntityHead head) {
        jdbcClient
                .sql(UPDATE)
                .param("userId", head.userId())
                .param("entityType", head.entityType())
                .param("entityId", head.entityId())
                .param("parentId", head.parentId())
                .param("version", head.version())
                .param("operationId", head.winningOperationId())
                .param("eventAt", Timestamp.from(head.order().eventAt()))
                .param("eventCounter", head.order().logicalCounter())
                .param("deviceId", head.order().deviceId())
                .param("deleted", head.deleted())
                .update();
    }

    public void deleteChildren(UUID userId, UUID deckId, EventOrder order) {
        jdbcClient
                .sql(DELETE_CHILDREN)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("operationId", order.operationId())
                .param("eventAt", Timestamp.from(order.eventAt()))
                .param("eventCounter", order.logicalCounter())
                .param("deviceId", order.deviceId())
                .update();
    }

    private SyncEntityHead map(ResultSet resultSet, int row) throws SQLException {
        UUID operationId = (UUID) resultSet.getObject("winning_operation_id");
        EventOrder order = operationId == null
                ? null
                : new EventOrder(
                        instant(resultSet, "event_at"),
                        resultSet.getInt("event_counter"),
                        (UUID) resultSet.getObject("event_device_id"),
                        operationId);
        return new SyncEntityHead(
                (UUID) resultSet.getObject("user_id"),
                resultSet.getString("entity_type"),
                (UUID) resultSet.getObject("entity_id"),
                (UUID) resultSet.getObject("parent_id"),
                resultSet.getInt("version"),
                operationId,
                order,
                resultSet.getBoolean("deleted"));
    }
}

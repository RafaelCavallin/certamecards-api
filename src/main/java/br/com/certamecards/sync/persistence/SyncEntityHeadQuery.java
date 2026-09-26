package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.SyncEntityHead;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SyncEntityHeadQuery {

    private static final String FIND =
            """
            SELECT user_id, entity_type, entity_id, parent_id, version, winning_operation_id, event_at,
                   event_counter, event_device_id, deleted
            FROM sync_entity_heads WHERE user_id = :userId AND entity_type = :entityType AND entity_id = :entityId
            """;

    private final JdbcClient jdbcClient;

    public SyncEntityHeadQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<SyncEntityHead> find(UUID userId, String entityType, UUID entityId) {
        return jdbcClient
                .sql(FIND)
                .param("userId", userId)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .query(this::map)
                .optional();
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

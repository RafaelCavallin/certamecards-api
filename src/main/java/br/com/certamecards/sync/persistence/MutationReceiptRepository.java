package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationReceipt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class MutationReceiptRepository {

    private static final String FIND_SQL =
            """
            SELECT user_id, operation_id, request_hash, kind, event_at, event_counter, event_device_id,
                   outcome, entity_version, change_seq, conflict_id, error_code, created_at
            FROM sync_operation_receipts
            WHERE user_id = :userId AND operation_id = :operationId
            """;

    private static final String INSERT_SQL =
            """
            INSERT INTO sync_operation_receipts
                (user_id, operation_id, request_hash, kind, event_at, event_counter, event_device_id,
                 outcome, entity_version, change_seq, conflict_id, error_code, created_at)
            VALUES
                (:userId, :operationId, :requestHash, :kind, :eventAt, :eventCounter, :eventDeviceId,
                 :outcome, :entityVersion, :changeSeq, :conflictId, :errorCode, :createdAt)
            ON CONFLICT (user_id, operation_id) DO NOTHING
            """;

    private final JdbcClient jdbcClient;

    public MutationReceiptRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<MutationReceipt> find(UUID userId, UUID operationId) {
        return jdbcClient
                .sql(FIND_SQL)
                .param("userId", userId)
                .param("operationId", operationId)
                .query(this::mapRow)
                .optional();
    }

    public void insert(MutationReceipt receipt) {
        jdbcClient
                .sql(INSERT_SQL)
                .param("userId", receipt.userId())
                .param("operationId", receipt.operationId())
                .param("requestHash", receipt.requestHash())
                .param("kind", receipt.kind())
                .param("eventAt", Timestamp.from(receipt.order().eventAt()))
                .param("eventCounter", receipt.order().logicalCounter())
                .param("eventDeviceId", receipt.order().deviceId())
                .param("outcome", receipt.outcome())
                .param("entityVersion", receipt.entityVersion())
                .param("changeSeq", receipt.changeSeq())
                .param("conflictId", receipt.conflictId())
                .param("errorCode", receipt.errorCode())
                .param("createdAt", Timestamp.from(receipt.createdAt()))
                .update();
    }

    private MutationReceipt mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID operationId = (UUID) rs.getObject("operation_id");
        EventOrder order = new EventOrder(
                instant(rs, "event_at"),
                rs.getInt("event_counter"),
                (UUID) rs.getObject("event_device_id"),
                operationId);
        return new MutationReceipt(
                (UUID) rs.getObject("user_id"),
                operationId,
                rs.getString("request_hash"),
                rs.getString("kind"),
                order,
                rs.getString("outcome"),
                entityVersionOf(rs.getObject("entity_version")),
                changeSeqOf(rs.getObject("change_seq")),
                (UUID) rs.getObject("conflict_id"),
                rs.getString("error_code"),
                instant(rs, "created_at"));
    }

    private Integer entityVersionOf(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Long changeSeqOf(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}

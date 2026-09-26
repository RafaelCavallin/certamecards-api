package br.com.certamecards.sync.persistence;

import br.com.certamecards.common.config.SyncProperties;
import br.com.certamecards.sync.domain.ConflictReason;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SyncConflictWriter {

    private static final String INSERT =
            """
            INSERT INTO sync_conflicts (user_id, entity_type, entity_id, deck_id, losing_operation_id,
                winning_operation_id, reason, losing_snapshot, winning_snapshot, expires_at)
            VALUES (:userId, :entityType, :entityId, :deckId, :losingOperationId, :winningOperationId,
                :reason, cast(:losingSnapshot as jsonb), cast(:winningSnapshot as jsonb),
                now() + make_interval(days => :retentionDays))
            ON CONFLICT (losing_operation_id) DO NOTHING
            RETURNING id
            """;
    private final JdbcClient jdbcClient;
    private final SyncProperties properties;

    public SyncConflictWriter(JdbcClient jdbcClient, SyncProperties properties) {
        this.jdbcClient = jdbcClient;
        this.properties = properties;
    }

    public UUID write(
            UUID userId,
            String type,
            UUID entityId,
            UUID deckId,
            UUID losingOperationId,
            UUID winningOperationId,
            ConflictReason reason,
            String losingSnapshot,
            String winningSnapshot) {
        return jdbcClient
                .sql(INSERT)
                .param("userId", userId)
                .param("entityType", type)
                .param("entityId", entityId)
                .param("deckId", deckId)
                .param("losingOperationId", losingOperationId)
                .param("winningOperationId", winningOperationId)
                .param("reason", reason.code())
                .param("losingSnapshot", losingSnapshot)
                .param("winningSnapshot", winningSnapshot)
                .param("retentionDays", properties.conflictRetentionDays())
                .query(UUID.class)
                .optional()
                .orElse(null);
    }
}

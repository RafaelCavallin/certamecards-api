package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.SyncMutationOperation;
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
                :reason, cast(:losingSnapshot as jsonb), cast(:winningSnapshot as jsonb), now() + interval '30 days')
            ON CONFLICT (losing_operation_id) DO NOTHING
            RETURNING id
            """;
    private final JdbcClient jdbcClient;

    public SyncConflictWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UUID write(
            UUID userId,
            String type,
            SyncMutationOperation losing,
            UUID winner,
            String reason,
            String losingSnapshot,
            String winningSnapshot) {
        return jdbcClient
                .sql(INSERT)
                .param("userId", userId)
                .param("entityType", type)
                .param("entityId", losing.entityId())
                .param("deckId", losing.parentId())
                .param("losingOperationId", losing.operationId())
                .param("winningOperationId", winner)
                .param("reason", reason)
                .param("losingSnapshot", losingSnapshot)
                .param("winningSnapshot", winningSnapshot)
                .query(UUID.class)
                .optional()
                .orElse(null);
    }
}

package br.com.certamecards.sync.persistence;

import static br.com.certamecards.common.persistence.CardStateSnapshotSql.instantOf;
import static br.com.certamecards.common.persistence.CardStateSnapshotSql.snapshot;

import br.com.certamecards.common.persistence.CardStateSnapshotSql;
import br.com.certamecards.common.sync.EventOrder;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DeckResetLogWriter {

    private static final String NEW_SNAPSHOT = snapshot(
            "0",
            new String[] {"0", "0"},
            instantOf("CAST(:eventAt AS timestamptz)"),
            new String[] {"CAST(NULL AS text)", "0", "0"},
            new String[] {"0", "0"});
    private static final String SQL =
            """
            WITH targets AS (
                SELECT cs.* FROM card_states cs JOIN cards c ON c.id = cs.card_id
                WHERE cs.user_id = :userId AND c.deck_id = :deckId AND c.deleted_at IS NULL AND cs.state <> 0
                FOR UPDATE OF cs),
            reset_states AS (
                UPDATE card_states cs SET state = 0, stability = 0, difficulty = 0, due = :eventAt, last_review = NULL,
                    reps = 0, lapses = 0, learning_steps = 0, scheduled_days = 0,
                    review_count = cs.review_count + 1, content_update_note = NULL, content_updated_at = NULL,
                    updated_at = :now
                FROM targets t WHERE cs.user_id = t.user_id AND cs.card_id = t.card_id RETURNING cs.card_id)
            INSERT INTO review_logs (id, user_id, card_id, kind, rating, reviewed_at, duration_ms,
                                     state_before, state_after, offline, device_id, received_at,
                                     event_at, event_counter, event_device_id, operation_id)
            SELECT gen.log_id, t.user_id, t.card_id, 'reset', NULL, :eventAt, 0,\s"""
                    + CardStateSnapshotSql.of("t")
                    + ", "
                    + NEW_SNAPSHOT
                    + ", false, :eventDeviceId, :now, :eventAt, :eventCounter, :eventDeviceId,"
                    + " gen.log_id FROM targets t CROSS JOIN LATERAL (SELECT uuidv7() AS log_id) gen";

    private final JdbcClient jdbcClient;

    public DeckResetLogWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int materialize(UUID userId, UUID deckId, EventOrder order, Instant now) {
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("eventAt", Timestamp.from(order.eventAt()))
                .param("eventCounter", order.logicalCounter())
                .param("eventDeviceId", order.deviceId())
                .param("now", Timestamp.from(now))
                .update();
    }
}

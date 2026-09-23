package br.com.certamecards.officialdeck.persistence;

import static br.com.certamecards.common.persistence.CardStateSnapshotSql.SYSTEM_DEVICE_UUID;
import static br.com.certamecards.common.persistence.CardStateSnapshotSql.instantOf;
import static br.com.certamecards.common.persistence.CardStateSnapshotSql.snapshot;

import br.com.certamecards.common.persistence.CardStateSnapshotSql;
import br.com.certamecards.officialdeck.domain.PurgeTarget;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ProgressPurgeWriter {

    private static final String NEW_SNAPSHOT = snapshot(
            "0",
            new String[] {"0", "0"},
            instantOf("CAST(:now AS timestamptz)"),
            new String[] {"CAST(NULL AS text)", "0", "0"},
            new String[] {"0", "0"});
    private static final String RESET_SQL =
            """
            WITH targets AS (
                SELECT cs.* FROM card_states cs JOIN cards c ON c.id = cs.card_id
                WHERE cs.user_id = :userId AND c.deck_id = :deckId AND (cs.state <> 0 OR cs.suspended)
                FOR UPDATE OF cs),
            reset_states AS (
                UPDATE card_states cs SET state = 0, stability = 0, difficulty = 0, due = :now, last_review = NULL,
                    reps = 0, lapses = 0, learning_steps = 0, scheduled_days = 0, suspended = false,
                    review_count = cs.review_count + CASE WHEN t.state <> 0 THEN 1 ELSE 0 END,
                    content_update_note = NULL, content_updated_at = NULL, updated_at = :now
                FROM targets t WHERE cs.user_id = t.user_id AND cs.card_id = t.card_id RETURNING cs.card_id)
            INSERT INTO review_logs (id, user_id, card_id, kind, rating, reviewed_at, duration_ms,
                                     state_before, state_after, offline, device_id, received_at,
                                     event_at, event_counter, event_device_id, operation_id)
            SELECT gen.log_id, t.user_id, t.card_id, 'reset', NULL, :now, 0,\s"""
                    + CardStateSnapshotSql.of("t")
                    + ", "
                    + NEW_SNAPSHOT
                    + ", false, "
                    + SYSTEM_DEVICE_UUID
                    + ", :now, :now, 0, "
                    + SYSTEM_DEVICE_UUID
                    + ", gen.log_id FROM targets t CROSS JOIN LATERAL (SELECT uuidv7() AS log_id) gen"
                    + " WHERE t.state <> 0";
    private static final String MARK_PURGED_SQL =
            "UPDATE deck_subscriptions SET progress_purged_at = :now WHERE user_id = :userId AND deck_id = :deckId";

    private final JdbcClient jdbcClient;

    public ProgressPurgeWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int reset(PurgeTarget target, Instant now) {
        return jdbcClient
                .sql(RESET_SQL)
                .param("userId", target.userId())
                .param("deckId", target.deckId())
                .param("now", Timestamp.from(now))
                .update();
    }

    public void markPurged(PurgeTarget target, Instant now) {
        jdbcClient
                .sql(MARK_PURGED_SQL)
                .param("userId", target.userId())
                .param("deckId", target.deckId())
                .param("now", Timestamp.from(now))
                .update();
    }
}

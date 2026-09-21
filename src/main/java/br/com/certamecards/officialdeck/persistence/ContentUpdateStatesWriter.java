package br.com.certamecards.officialdeck.persistence;

import static br.com.certamecards.common.persistence.CardStateSnapshotSql.SYSTEM_DEVICE_UUID;
import static br.com.certamecards.common.persistence.CardStateSnapshotSql.instantOf;
import static br.com.certamecards.common.persistence.CardStateSnapshotSql.snapshot;

import br.com.certamecards.common.persistence.CardStateSnapshotSql;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateStatesWriter {

    private static final int RELEARNING_STATE = 3;
    private static final String AFTER_SNAPSHOT = snapshot(
            String.valueOf(RELEARNING_STATE),
            new String[] {"l.stability", "l.difficulty"},
            instantOf("CAST(:updatedAt AS timestamptz)"),
            new String[] {instantOf("l.last_review"), "l.reps", "l.lapses"},
            new String[] {"0", "0"});
    private static final String SQL =
            """
            WITH locked AS (
                SELECT * FROM card_states
                WHERE card_id = :cardId AND user_id IN (:userIds) AND state <> 0 FOR UPDATE),
            updated AS (
                UPDATE card_states cs SET state = 3, learning_steps = 0, scheduled_days = 0, due = :updatedAt,
                    review_count = cs.review_count + 1, content_update_note = :note,
                    content_updated_at = :updatedAt, updated_at = :now
                FROM locked l WHERE cs.user_id = l.user_id AND cs.card_id = l.card_id RETURNING cs.user_id)
            INSERT INTO review_logs (id, user_id, card_id, kind, rating, reviewed_at, duration_ms,
                                     state_before, state_after, offline, device_id, received_at)
            SELECT uuidv7(), l.user_id, l.card_id, 'content_update', NULL, :updatedAt, 0,\s"""
                    + CardStateSnapshotSql.of("l")
                    + ", "
                    + AFTER_SNAPSHOT
                    + ", false, "
                    + SYSTEM_DEVICE_UUID
                    + ", :now FROM locked l";

    private final JdbcClient jdbcClient;

    public ContentUpdateStatesWriter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int apply(ContentUpdateJob job, List<UUID> userIds, Instant now) {
        return jdbcClient
                .sql(SQL)
                .param("cardId", job.cardId())
                .param("userIds", userIds)
                .param("updatedAt", Timestamp.from(job.updatedAt()))
                .param("note", job.note())
                .param("now", Timestamp.from(now))
                .update();
    }
}

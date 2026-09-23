package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.ChangeEntry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class GlobalChangesQuery {

    private static final String SQL =
            """
            SELECT change_seq, type, payload::text AS payload FROM (
                SELECT s.change_seq AS change_seq, 'subject'::text AS type,
                       jsonb_build_object('id', s.id, 'name', s.name, 'active', s.active) AS payload
                FROM subjects s
                WHERE s.change_seq > :cursor AND s.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT d.change_seq, 'deck'::text,
                       jsonb_build_object(
                           'id', d.id, 'subjectId', d.subject_id, 'name', d.name, 'description', d.description,
                           'origin', d.origin, 'originRef', d.origin_ref, 'officialStatus', d.official_status,
                           'originLabel', d.origin_label, 'contentUpdatedAt', d.content_updated_at,
                           'cardCount', d.card_count, 'createdAt', d.created_at, 'updatedAt', d.updated_at,
                           'deletedAt', d.deleted_at, 'version', d.version)
                FROM decks d
                WHERE (d.owner_id = :userId OR EXISTS (
                           SELECT 1 FROM deck_subscriptions sub
                           WHERE sub.deck_id = d.id AND sub.user_id = :userId AND sub.cancelled_at IS NULL))
                  AND d.change_seq > :cursor AND d.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT c.change_seq, 'card'::text,
                       jsonb_build_object(
                           'id', c.id, 'deckId', c.deck_id, 'type', c.type, 'front', c.front, 'back', c.back,
                           'source', c.source, 'createdAt', c.created_at, 'updatedAt', c.updated_at,
                           'deletedAt', c.deleted_at, 'version', c.version)
                FROM cards c JOIN decks d2 ON d2.id = c.deck_id
                WHERE (d2.owner_id = :userId OR EXISTS (
                           SELECT 1 FROM deck_subscriptions sub2
                           WHERE sub2.deck_id = d2.id AND sub2.user_id = :userId AND sub2.cancelled_at IS NULL))
                  AND c.change_seq > :cursor AND c.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT cs.change_seq, 'card_state'::text,
                       jsonb_build_object(
                           'cardId', cs.card_id, 'state', cs.state, 'stability', cs.stability,
                           'difficulty', cs.difficulty, 'due', cs.due, 'lastReview', cs.last_review,
                           'reps', cs.reps, 'lapses', cs.lapses, 'learningSteps', cs.learning_steps,
                           'scheduledDays', cs.scheduled_days, 'reviewCount', cs.review_count,
                           'suspended', cs.suspended, 'contentUpdateNote', cs.content_update_note,
                           'contentUpdatedAt', cs.content_updated_at)
                FROM card_states cs
                WHERE cs.user_id = :userId AND cs.change_seq > :cursor
                  AND cs.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT rl.change_seq, 'review_log'::text,
                       jsonb_build_object(
                           'id', rl.id, 'cardId', rl.card_id, 'kind', rl.kind, 'rating', rl.rating,
                           'reviewedAt', rl.reviewed_at, 'durationMs', rl.duration_ms,
                           'stateBefore', rl.state_before, 'stateAfter', rl.state_after, 'offline', rl.offline,
                           'deviceId', rl.device_id, 'sessionId', rl.session_id)
                FROM review_logs rl
                WHERE rl.user_id = :userId AND rl.change_seq > :cursor
                  AND rl.write_xid < pg_snapshot_xmin(pg_current_snapshot())
                  AND (:cursor > 0 OR rl.reviewed_at > :windowStart)

                UNION ALL

                SELECT rv.change_seq, 'review_void'::text,
                       jsonb_build_object('reviewId', rv.review_id, 'voidedAt', rv.voided_at)
                FROM review_voids rv JOIN review_logs rl2 ON rl2.id = rv.review_id
                WHERE rl2.user_id = :userId AND rv.change_seq > :cursor
                  AND rv.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT sub3.change_seq, 'subscription'::text,
                       jsonb_build_object(
                           'deckId', sub3.deck_id, 'subscribedAt', sub3.subscribed_at,
                           'cancelledAt', sub3.cancelled_at)
                FROM deck_subscriptions sub3
                WHERE sub3.user_id = :userId AND sub3.change_seq > :cursor
                  AND sub3.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT us.change_seq, 'settings'::text,
                       jsonb_build_object(
                           'newPerDay', us.new_per_day, 'reviewsPerDay', us.reviews_per_day,
                           'focusMinutes', us.focus_minutes, 'examDate', us.exam_date,
                           'timeZone', us.time_zone, 'theme', us.theme)
                FROM user_settings us
                WHERE us.user_id = :userId AND us.change_seq > :cursor
                  AND us.write_xid < pg_snapshot_xmin(pg_current_snapshot())

                UNION ALL

                SELECT u.change_seq, 'profile'::text,
                       jsonb_build_object('id', u.id, 'displayName', u.display_name)
                FROM users u
                WHERE u.id = :userId AND u.change_seq > :cursor
                  AND u.write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ) combined
            ORDER BY change_seq
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public GlobalChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ChangeEntry> fetch(UUID userId, long cursor, int limit, Instant reviewLogWindowStart) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("windowStart", Timestamp.from(reviewLogWindowStart))
                .param("limit", limit)
                .query((rs, rowNum) -> new ChangeEntry(
                        rs.getLong("change_seq"), rs.getString("type"), new RawValue(rs.getString("payload"))))
                .list();
    }
}

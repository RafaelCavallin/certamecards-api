package br.com.certamecards.library.persistence;

import static br.com.certamecards.common.persistence.CardStateSnapshotSql.SYSTEM_DEVICE_UUID;

import br.com.certamecards.common.persistence.CardStateSnapshotSql;

final class DeckCopySql {

    static final String INSERT_DECK =
            """
            INSERT INTO decks (id, owner_id, subject_id, name, description, origin, origin_ref, origin_label,
                               created_at, updated_at)
            VALUES (:newDeckId, :userId, :subjectId, :name, :description, 'official_copy', :sourceId, :name,
                    :now, :now)
            RETURNING change_seq
            """;

    static final String COPY_CARDS =
            """
            WITH src AS (
                SELECT id AS old_id, uuidv7() AS new_id, type, front, back, source
                FROM cards WHERE deck_id = :sourceId AND deleted_at IS NULL),
            ins_cards AS (
                INSERT INTO cards (id, deck_id, type, front, back, source, created_at, updated_at)
                SELECT new_id, :newDeckId, type, front, back, source, :now, :now FROM src RETURNING id),
            carried AS (
                SELECT s.new_id, cs.state, cs.stability, cs.difficulty, cs.due, cs.last_review, cs.reps,
                       cs.lapses, cs.learning_steps, cs.scheduled_days, cs.suspended
                FROM src s JOIN card_states cs ON cs.card_id = s.old_id AND cs.user_id = :userId
                WHERE :carry AND (cs.state <> 0 OR cs.suspended)),
            ins_states AS (
                INSERT INTO card_states (user_id, card_id, state, stability, difficulty, due, last_review, reps,
                                         lapses, learning_steps, scheduled_days, review_count, suspended, updated_at)
                SELECT :userId, new_id, state, stability, difficulty, due, last_review, reps, lapses,
                       learning_steps, scheduled_days, CASE WHEN state = 0 THEN 0 ELSE 1 END, suspended, :now
                FROM carried RETURNING card_id),
            ins_logs AS (
                INSERT INTO review_logs (id, user_id, card_id, kind, rating, reviewed_at, duration_ms,
                                         state_before, state_after, offline, device_id, received_at)
                SELECT uuidv7(), :userId, c.new_id, 'duplicate', NULL, :now, 0, NULL,\s"""
                    + CardStateSnapshotSql.of("c")
                    + ", false, "
                    + SYSTEM_DEVICE_UUID
                    + ", :now FROM carried c WHERE c.state <> 0 RETURNING id) "
                    + "SELECT (SELECT count(*) FROM ins_cards) AS copied_cards, "
                    + "(SELECT count(*) FROM ins_states) AS carried_states, "
                    + "(SELECT count(*) FROM ins_logs) AS seed_logs";

    static final String DESCRIBE =
            """
            SELECT (SELECT count(*) FROM cards WHERE deck_id = :deckId AND deleted_at IS NULL) AS copied_cards,
                   (SELECT count(*) FROM card_states cs JOIN cards c ON c.id = cs.card_id
                    WHERE cs.user_id = :userId AND c.deck_id = :deckId) AS carried_states
            """;

    private DeckCopySql() {}
}

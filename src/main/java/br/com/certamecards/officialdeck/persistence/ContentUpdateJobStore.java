package br.com.certamecards.officialdeck.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateJobStore {

    private static final String ENQUEUE_SQL =
            "INSERT INTO official_content_update_jobs (card_id, deck_id, note, updated_at) "
                    + "VALUES (:cardId, :deckId, :note, :updatedAt)";
    private static final String PENDING_SQL =
            "SELECT id FROM official_content_update_jobs WHERE finished_at IS NULL ORDER BY created_at, id LIMIT :limit";
    private static final String LOCK_SQL =
            "SELECT id, card_id, deck_id, note, updated_at, cursor_user_id FROM official_content_update_jobs "
                    + "WHERE id = :id AND finished_at IS NULL FOR UPDATE SKIP LOCKED";
    private static final String ADVANCE_SQL = "UPDATE official_content_update_jobs SET cursor_user_id = :cursor, "
            + "applied_count = applied_count + :applied WHERE id = :id";
    private static final String FINISH_SQL =
            "UPDATE official_content_update_jobs SET finished_at = :now WHERE id = :id";

    private final JdbcClient jdbcClient;

    public ContentUpdateJobStore(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void enqueue(ContentUpdateJob job) {
        jdbcClient
                .sql(ENQUEUE_SQL)
                .param("cardId", job.cardId())
                .param("deckId", job.deckId())
                .param("note", job.note())
                .param("updatedAt", Timestamp.from(job.updatedAt()))
                .update();
    }

    public List<UUID> pendingIds(int limit) {
        return jdbcClient
                .sql(PENDING_SQL)
                .param("limit", limit)
                .query(UUID.class)
                .list();
    }

    public Optional<ContentUpdateJob> lockPending(UUID id) {
        return jdbcClient
                .sql(LOCK_SQL)
                .param("id", id)
                .query((rs, rowNum) -> new ContentUpdateJob(
                        (UUID) rs.getObject("id"),
                        (UUID) rs.getObject("card_id"),
                        (UUID) rs.getObject("deck_id"),
                        rs.getString("note"),
                        instant(rs, "updated_at"),
                        (UUID) rs.getObject("cursor_user_id")))
                .optional();
    }

    public void advance(UUID id, UUID cursor, int applied) {
        jdbcClient
                .sql(ADVANCE_SQL)
                .param("id", id)
                .param("cursor", cursor)
                .param("applied", applied)
                .update();
    }

    public void finish(UUID id, Instant now) {
        jdbcClient
                .sql(FINISH_SQL)
                .param("id", id)
                .param("now", Timestamp.from(now))
                .update();
    }
}

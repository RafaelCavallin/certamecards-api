package br.com.certamecards.sync.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.config.SyncProperties;
import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.sync.domain.ConflictCursor;
import br.com.certamecards.sync.domain.ConflictReason;
import br.com.certamecards.sync.domain.SyncConflict;
import br.com.certamecards.sync.domain.SyncEntityHead;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class SyncConflictRepositoryIT {

    @Autowired
    private SyncConflictRepository repository;

    @Autowired
    private SyncConflictWriter writer;

    @Autowired
    private SyncEntityHeadQuery headQuery;

    @Autowired
    private ConflictPurgeQuery purgeQuery;

    @Autowired
    private SyncProperties properties;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void givenNoCursor_whenListing_thenReturnsFirstPage() {
        UUID userId = createUser();
        UUID conflictId = writeConflict(userId);

        List<SyncConflict> page = repository.list(userId, null, 10);

        assertThat(page).extracting(SyncConflict::id).contains(conflictId);
    }

    @Test
    void givenCursorPastExpiry_whenListing_thenExcludesEarlierConflict() {
        UUID userId = createUser();
        writeConflict(userId);
        Instant farFuture = Instant.now().plusSeconds(60L * 60 * 24 * 400);

        List<SyncConflict> page = repository.list(userId, new ConflictCursor(farFuture, new UUID(0L, 0L)), 10);

        assertThat(page).isEmpty();
    }

    @Test
    void givenExpiredConflict_whenPurgedAndFound_thenSnapshotsAreNull() {
        UUID userId = createUser();
        UUID conflictId = writeConflict(userId);
        Instant farFuture = Instant.now().plusSeconds(properties.conflictRetentionDays() * 24L * 3600 + 3600);

        purgeQuery.expireDueConflicts(farFuture);
        Optional<SyncConflict> found = repository.find(userId, conflictId);

        assertThat(found).isPresent();
        assertThat(found.get().losingSnapshot()).isNull();
        assertThat(found.get().winningSnapshot()).isNull();
        assertThat(found.get().expiredAt()).isNotNull();
    }

    @Test
    void givenHeadNeverWon_whenFindingHead_thenOrderIsNull() {
        UUID userId = createUser();
        UUID entityId = UUID.randomUUID();
        jdbcClient
                .sql("INSERT INTO sync_entity_heads (user_id, entity_type, entity_id) "
                        + "VALUES (:userId, 'deck', :entityId)")
                .param("userId", userId)
                .param("entityId", entityId)
                .update();

        Optional<SyncEntityHead> head = headQuery.find(userId, "deck", entityId);

        assertThat(head).isPresent();
        assertThat(head.get().winningOperationId()).isNull();
        assertThat(head.get().order()).isNull();
    }

    private UUID writeConflict(UUID userId) {
        UUID entityId = UUID.randomUUID();
        UUID losingOperationId = UUID.randomUUID();
        UUID winningOperationId = UUID.randomUUID();
        return writer.write(
                userId,
                "deck",
                entityId,
                null,
                losingOperationId,
                winningOperationId,
                ConflictReason.CONCURRENT_EDIT,
                "{\"name\":\"Perdedora\"}",
                "{\"name\":\"Vencedora\"}");
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbcClient
                .sql("INSERT INTO users (id, email, display_name) VALUES (:id, :email, 'Conflito')")
                .param("id", userId)
                .param("email", userId + "@exemplo.com")
                .update();
        return userId;
    }
}

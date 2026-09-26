package br.com.certamecards.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.sync.EventClock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class SyncEntityHeadTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void givenWinningOperationIdNull_whenCheckingCausalSuccessor_thenFalse() {
        SyncEntityHead head = head(null);

        assertThat(head.isCausalSuccessor(operation(UUID.randomUUID()))).isFalse();
    }

    @Test
    void givenMatchingPredecessor_whenCheckingCausalSuccessor_thenTrue() {
        UUID winner = UUID.randomUUID();
        SyncEntityHead head = head(winner);

        assertThat(head.isCausalSuccessor(operation(winner))).isTrue();
    }

    @Test
    void givenNonMatchingPredecessor_whenCheckingCausalSuccessor_thenFalse() {
        SyncEntityHead head = head(UUID.randomUUID());

        assertThat(head.isCausalSuccessor(operation(UUID.randomUUID()))).isFalse();
    }

    @Test
    void givenNullPredecessor_whenCheckingCausalSuccessor_thenFalse() {
        SyncEntityHead head = head(UUID.randomUUID());

        assertThat(head.isCausalSuccessor(operation(null))).isFalse();
    }

    @Test
    void givenMatchingVersion_whenCheckingBaseVersion_thenTrue() {
        SyncEntityHead head = new SyncEntityHead(USER_ID, "deck", UUID.randomUUID(), null, 3, null, null, false);

        assertThat(head.hasBaseVersion(operationWithBaseVersion(3))).isTrue();
    }

    @Test
    void givenMismatchedVersion_whenCheckingBaseVersion_thenFalse() {
        SyncEntityHead head = new SyncEntityHead(USER_ID, "deck", UUID.randomUUID(), null, 3, null, null, false);

        assertThat(head.hasBaseVersion(operationWithBaseVersion(2))).isFalse();
    }

    @Test
    void givenNullBaseVersion_whenCheckingBaseVersion_thenFalse() {
        SyncEntityHead head = new SyncEntityHead(USER_ID, "deck", UUID.randomUUID(), null, 3, null, null, false);

        assertThat(head.hasBaseVersion(operationWithBaseVersion(null))).isFalse();
    }

    private SyncEntityHead head(UUID winningOperationId) {
        return new SyncEntityHead(USER_ID, "deck", UUID.randomUUID(), null, 0, winningOperationId, null, false);
    }

    private SyncMutationOperation operation(UUID predecessorOperationId) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_UPDATE,
                UUID.randomUUID(),
                null,
                null,
                predecessorOperationId,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private SyncMutationOperation operationWithBaseVersion(Integer baseVersion) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_UPDATE,
                UUID.randomUUID(),
                null,
                baseVersion,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }
}

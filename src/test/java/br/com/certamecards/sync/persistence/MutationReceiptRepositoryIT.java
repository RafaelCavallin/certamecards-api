package br.com.certamecards.sync.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.MutationReceipt;
import java.time.Instant;
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
class MutationReceiptRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-09-21T14:00:00Z");

    @Autowired
    private MutationReceiptRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void givenNoReceipt_whenFinding_thenReturnsEmpty() {
        UUID userId = createUser();

        Optional<MutationReceipt> found = repository.find(userId, UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void givenReceiptWithNullOptionalFields_whenInsertedAndFound_thenFieldsRoundTripAsNull() {
        UUID userId = createUser();
        UUID operationId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        MutationReceipt receipt = new MutationReceipt(
                userId,
                operationId,
                "hash-a",
                "deck_create",
                new EventOrder(NOW, 0, deviceId, operationId),
                "applied",
                null,
                null,
                null,
                null,
                NOW);

        repository.insert(receipt);
        Optional<MutationReceipt> found = repository.find(userId, operationId);

        assertThat(found).isPresent();
        assertThat(found.get().entityVersion()).isNull();
        assertThat(found.get().changeSeq()).isNull();
        assertThat(found.get().conflictId()).isNull();
        assertThat(found.get().errorCode()).isNull();
        assertThat(found.get().outcome()).isEqualTo("applied");
        assertThat(found.get().requestHash()).isEqualTo("hash-a");
    }

    @Test
    void givenReceiptWithAllFieldsFilled_whenInsertedAndFound_thenFieldsRoundTrip() {
        UUID userId = createUser();
        UUID operationId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID conflictId = UUID.randomUUID();
        MutationReceipt receipt = new MutationReceipt(
                userId,
                operationId,
                "hash-b",
                "card_update",
                new EventOrder(NOW, 3, deviceId, operationId),
                "conflict",
                7,
                42L,
                conflictId,
                "conflict_expired",
                NOW);

        repository.insert(receipt);
        Optional<MutationReceipt> found = repository.find(userId, operationId);

        assertThat(found).isPresent();
        assertThat(found.get().entityVersion()).isEqualTo(7);
        assertThat(found.get().changeSeq()).isEqualTo(42L);
        assertThat(found.get().conflictId()).isEqualTo(conflictId);
        assertThat(found.get().errorCode()).isEqualTo("conflict_expired");
        assertThat(found.get().order().logicalCounter()).isEqualTo(3);
        assertThat(found.get().order().deviceId()).isEqualTo(deviceId);
    }

    @Test
    void givenExistingReceipt_whenInsertingSameOperationIdAgain_thenOnConflictDoNothingKeepsOriginal() {
        UUID userId = createUser();
        UUID operationId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        MutationReceipt original = new MutationReceipt(
                userId,
                operationId,
                "hash-first",
                "deck_create",
                new EventOrder(NOW, 0, deviceId, operationId),
                "applied",
                1,
                10L,
                null,
                null,
                NOW);
        MutationReceipt attempted = new MutationReceipt(
                userId,
                operationId,
                "hash-second",
                "deck_create",
                new EventOrder(NOW, 0, deviceId, operationId),
                "applied",
                2,
                20L,
                null,
                null,
                NOW);

        repository.insert(original);
        repository.insert(attempted);
        Optional<MutationReceipt> found = repository.find(userId, operationId);

        assertThat(found).isPresent();
        assertThat(found.get().requestHash()).isEqualTo("hash-first");
        assertThat(found.get().entityVersion()).isEqualTo(1);
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbcClient
                .sql("INSERT INTO users (id, email, display_name) VALUES (:id, :email, 'Receipt')")
                .param("id", userId)
                .param("email", userId + "@exemplo.com")
                .update();
        return userId;
    }
}

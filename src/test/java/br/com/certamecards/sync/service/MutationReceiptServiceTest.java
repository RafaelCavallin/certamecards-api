package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationReceipt;
import br.com.certamecards.sync.persistence.MutationReceiptRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MutationReceiptServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OPERATION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-21T14:00:00Z");
    private static final EventOrder ORDER = new EventOrder(NOW, 0, UUID.randomUUID(), OPERATION_ID);

    private final MutationReceiptRepository repository = mock(MutationReceiptRepository.class);
    private final MutationReceiptService service = new MutationReceiptService(repository);

    @Test
    void givenNoExistingReceipt_whenReserving_thenInsertsAndReturnsNewReceipt() {
        when(repository.find(USER_ID, OPERATION_ID)).thenReturn(Optional.empty());

        MutationReceipt receipt = service.reserve(
                USER_ID, OPERATION_ID, "hash-a", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW);

        assertThat(receipt.outcome()).isEqualTo("applied");
        assertThat(receipt.requestHash()).isEqualTo("hash-a");
        verify(repository).insert(any(MutationReceipt.class));
    }

    @Test
    void givenExistingReceiptWithSameHash_whenReserving_thenReturnsExistingWithoutInserting() {
        MutationReceipt existing = new MutationReceipt(
                USER_ID, OPERATION_ID, "hash-a", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW);
        when(repository.find(USER_ID, OPERATION_ID)).thenReturn(Optional.of(existing));

        MutationReceipt receipt = service.reserve(
                USER_ID, OPERATION_ID, "hash-a", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW);

        assertThat(receipt).isEqualTo(existing);
        verify(repository, never()).insert(any(MutationReceipt.class));
    }

    @Test
    void givenExistingReceiptWithDifferentHash_whenReserving_thenRejectsAsOperationIdReused() {
        MutationReceipt existing = new MutationReceipt(
                USER_ID, OPERATION_ID, "hash-a", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW);
        when(repository.find(USER_ID, OPERATION_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.reserve(
                        USER_ID, OPERATION_ID, "hash-b", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OPERATION_ID_REUSED);
        verify(repository, never()).insert(any(MutationReceipt.class));
    }

    @Test
    void givenOperationId_whenFinding_thenDelegatesToRepository() {
        MutationReceipt existing = new MutationReceipt(
                USER_ID, OPERATION_ID, "hash-a", "deck_create", ORDER, "applied", 1, 10L, null, null, NOW);
        when(repository.find(USER_ID, OPERATION_ID)).thenReturn(Optional.of(existing));

        Optional<MutationReceipt> found = service.find(USER_ID, OPERATION_ID);

        assertThat(found).contains(existing);
    }
}

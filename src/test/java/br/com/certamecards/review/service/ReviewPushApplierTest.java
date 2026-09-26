package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.review.domain.AppliedState;
import br.com.certamecards.review.domain.IgnoredState;
import br.com.certamecards.review.domain.StaleState;
import br.com.certamecards.review.persistence.ReviewVoidWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ReviewPushApplierTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final UUID DEVICE_ID = UUID.randomUUID();

    private final ReviewWriter reviewWriter = mock(ReviewWriter.class);
    private final ReviewVoidWriter voidWriter = mock(ReviewVoidWriter.class);
    private final CardStateReconciler stateReconciler = mock(CardStateReconciler.class);
    private final ReviewPushApplier applier = new ReviewPushApplier(reviewWriter, voidWriter, stateReconciler);

    @Test
    @DisplayName("TU-27 — aplica reviews, depois anulações, depois reconcilia estados, nessa ordem")
    void givenCommand_whenApplying_thenCallsCollaboratorsInOrderAndAssemblesOutcome() {
        ReviewPushContext context = new ReviewPushContext(UUID.randomUUID(), Map.of(), FIXED_NOW);
        ReviewPushCommand command = new ReviewPushCommand(DEVICE_ID, List.of(), List.of(), List.of());
        ReviewInsertOutcome reviewOutcome = new ReviewInsertOutcome(List.of(), List.of(), List.of());
        List<UUID> acceptedVoids = List.of(UUID.randomUUID());
        StateOutcome stateOutcome = new StateOutcome(
                List.of(new AppliedState(UUID.randomUUID(), 1L)),
                List.of(new StaleState(UUID.randomUUID(), 9L)),
                List.of(new IgnoredState(UUID.randomUUID(), "card_deleted")));
        when(reviewWriter.insertAll(any(), any(), any(), any())).thenReturn(reviewOutcome);
        when(voidWriter.insertAll(any(), any())).thenReturn(acceptedVoids);
        when(stateReconciler.reconcile(any())).thenReturn(stateOutcome);

        ReviewPushOutcome outcome = applier.apply(context, command);

        InOrder order = inOrder(reviewWriter, voidWriter, stateReconciler);
        order.verify(reviewWriter).insertAll(any(), any(), any(), any());
        order.verify(voidWriter).insertAll(any(), any());
        order.verify(stateReconciler).reconcile(any());
        assertThat(outcome.reviews()).isEqualTo(reviewOutcome);
        assertThat(outcome.acceptedVoids()).isEqualTo(acceptedVoids);
        assertThat(outcome.states()).isEqualTo(stateOutcome);
    }
}

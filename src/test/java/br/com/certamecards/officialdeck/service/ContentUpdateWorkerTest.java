package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentUpdateWorkerTest {

    private final ContentUpdateJobStore jobStore = mock(ContentUpdateJobStore.class);
    private final ContentUpdateBatchApplier applier = mock(ContentUpdateBatchApplier.class);
    private final ContentUpdateWorker worker = new ContentUpdateWorker(jobStore, applier);

    @Test
    void givenNoPendingJobs_whenRunning_thenProcessesNothing() {
        when(jobStore.pendingIds(10)).thenReturn(List.of());

        assertThat(worker.runPendingBatch()).isZero();
    }

    @Test
    void givenPendingJobs_whenRunning_thenCountsOnlyTheOnesActuallyProcessed() {
        UUID processed = UUID.randomUUID();
        UUID lockedElsewhere = UUID.randomUUID();
        when(jobStore.pendingIds(10)).thenReturn(List.of(processed, lockedElsewhere));
        when(applier.applyNextBatch(processed)).thenReturn(true);
        when(applier.applyNextBatch(lockedElsewhere)).thenReturn(false);

        assertThat(worker.runPendingBatch()).isEqualTo(1);
    }
}

package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.config.OfficialProperties;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import br.com.certamecards.officialdeck.persistence.ContentUpdateStatesWriter;
import br.com.certamecards.officialdeck.persistence.ContentUpdateSubscribersQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentUpdateBatchApplierTest {

    private static final Instant NOW = Instant.parse("2026-09-19T14:05:00Z");
    private static final int BATCH = 500;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ContentUpdateJobStore jobStore = mock(ContentUpdateJobStore.class);
    private final ContentUpdateSubscribersQuery subscribersQuery = mock(ContentUpdateSubscribersQuery.class);
    private final ContentUpdateStatesWriter statesWriter = mock(ContentUpdateStatesWriter.class);
    private final ContentUpdateBatchApplier applier = new ContentUpdateBatchApplier(
            jobStore,
            subscribersQuery,
            statesWriter,
            new OfficialProperties(BATCH, Duration.ofSeconds(1)),
            Clock.fixed(NOW, ZoneOffset.UTC),
            new OfficialMetrics(registry));
    private final UUID cursor = UUID.randomUUID();
    private final ContentUpdateJob job =
            new ContentUpdateJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "nota", NOW, cursor);

    @Test
    void givenTU48_whenJobIsLockedByAnotherWorkerOrFinished_thenNothingIsProcessed() {
        when(jobStore.lockPending(job.id())).thenReturn(Optional.empty());

        assertThat(applier.applyNextBatch(job.id())).isFalse();

        verify(subscribersQuery, never()).lockNextBatch(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void givenTU48_whenBatchHasSubscribers_thenAppliesAndAdvancesCursorToTheLastUserWithoutFinishing() {
        UUID first = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        when(jobStore.lockPending(job.id())).thenReturn(Optional.of(job));
        when(subscribersQuery.lockNextBatch(job, BATCH)).thenReturn(List.of(first, last));
        when(statesWriter.apply(job, List.of(first, last), NOW)).thenReturn(1);

        assertThat(applier.applyNextBatch(job.id())).isTrue();

        verify(jobStore).advance(job.id(), last, 1);
        verify(jobStore, never()).finish(any(), any());
    }

    @Test
    void givenTU48_whenNoSubscribersRemainAfterTheCursor_thenFinishesTheJobWithoutTouchingStates() {
        when(jobStore.lockPending(job.id())).thenReturn(Optional.of(job));
        when(subscribersQuery.lockNextBatch(job, BATCH)).thenReturn(List.of());

        assertThat(applier.applyNextBatch(job.id())).isTrue();

        verify(jobStore).finish(job.id(), NOW);
        verify(statesWriter, never()).apply(any(), any(), any());
    }

    @Test
    void givenTU48_whenResumingAJobWithACursor_thenTheQueryStartsFromThatCursor() {
        when(jobStore.lockPending(job.id())).thenReturn(Optional.of(job));
        when(subscribersQuery.lockNextBatch(job, BATCH)).thenReturn(List.of());

        applier.applyNextBatch(job.id());

        verify(subscribersQuery)
                .lockNextBatch(
                        org.mockito.ArgumentMatchers.argThat(j -> cursor.equals(j.cursorUserId())),
                        org.mockito.ArgumentMatchers.eq(BATCH));
    }

    @Test
    void givenBatchThenCompletion_whenApplying_thenCountsAppliedSubscribersAndFinishedLag() {
        UUID first = UUID.randomUUID();
        ContentUpdateJob slow =
                new ContentUpdateJob(job.id(), job.cardId(), job.deckId(), "nota", NOW.minusSeconds(300), cursor);
        when(jobStore.lockPending(slow.id())).thenReturn(Optional.of(slow));
        when(subscribersQuery.lockNextBatch(slow, BATCH)).thenReturn(List.of(first), List.of());
        when(statesWriter.apply(slow, List.of(first), NOW)).thenReturn(1);

        applier.applyNextBatch(slow.id());
        applier.applyNextBatch(slow.id());

        assertThat(registry.counter("official.content_update.applied").count()).isEqualTo(1);
        assertThat(registry.counter("official.content_update.jobs", "result", "finished")
                        .count())
                .isEqualTo(1);
        assertThat(registry.summary("official.content_update.lag_seconds").max())
                .isEqualTo(300);
    }
}

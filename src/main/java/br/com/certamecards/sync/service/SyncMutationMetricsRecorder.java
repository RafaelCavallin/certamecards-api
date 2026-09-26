package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SyncMutationMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public SyncMutationMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(List<SyncMutationOperation> operations, List<MutationResult> results, Instant receivedAt) {
        for (MutationResult result : results) {
            SyncMutationOperation operation = operations.stream()
                    .filter(candidate -> candidate.operationId().equals(result.operationId()))
                    .findFirst()
                    .orElseThrow();
            meterRegistry
                    .counter(
                            "sync.mutations.items",
                            "kind",
                            operation.kind().value(),
                            "outcome",
                            result.outcome().value())
                    .increment();
            meterRegistry
                    .summary(
                            "sync.mutations.lag.seconds",
                            "kind",
                            operation.kind().value())
                    .record(Duration.between(operation.occurredAt(), receivedAt).toSeconds());
        }
    }

    public void recordBatchDuration(Duration duration) {
        meterRegistry.timer("sync.mutations.batch.duration").record(duration);
    }
}

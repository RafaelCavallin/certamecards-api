package br.com.certamecards.officialdeck.service;

import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateWorker {

    private static final Logger log = LoggerFactory.getLogger(ContentUpdateWorker.class);
    private static final int JOBS_PER_RUN = 10;
    private static final int FAILURES_BEFORE_ERROR = 3;
    private static final String INTERVAL = "${certame.official.content-update-interval}";

    private final ContentUpdateJobStore jobStore;
    private final ContentUpdateBatchApplier batchApplier;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    public ContentUpdateWorker(ContentUpdateJobStore jobStore, ContentUpdateBatchApplier batchApplier) {
        this.jobStore = jobStore;
        this.batchApplier = batchApplier;
    }

    @Scheduled(fixedDelayString = INTERVAL, initialDelayString = INTERVAL)
    public int runPendingBatch() {
        int processedJobs = 0;
        for (UUID jobId : jobStore.pendingIds(JOBS_PER_RUN)) {
            if (applySafely(jobId)) {
                processedJobs++;
            }
        }
        return processedJobs;
    }

    private boolean applySafely(UUID jobId) {
        try {
            boolean applied = batchApplier.applyNextBatch(jobId);
            consecutiveFailures.set(0);
            return applied;
        } catch (RuntimeException exception) {
            if (consecutiveFailures.incrementAndGet() >= FAILURES_BEFORE_ERROR) {
                log.error(
                        "Content update worker failed on {} consecutive batches", consecutiveFailures.get(), exception);
            }
            return false;
        }
    }
}

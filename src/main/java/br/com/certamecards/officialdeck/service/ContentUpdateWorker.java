package br.com.certamecards.officialdeck.service;

import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContentUpdateWorker {

    private static final int JOBS_PER_RUN = 10;
    private static final String INTERVAL = "${certame.official.content-update-interval}";

    private final ContentUpdateJobStore jobStore;
    private final ContentUpdateBatchApplier batchApplier;

    public ContentUpdateWorker(ContentUpdateJobStore jobStore, ContentUpdateBatchApplier batchApplier) {
        this.jobStore = jobStore;
        this.batchApplier = batchApplier;
    }

    @Scheduled(fixedDelayString = INTERVAL, initialDelayString = INTERVAL)
    public int runPendingBatch() {
        int processedJobs = 0;
        for (UUID jobId : jobStore.pendingIds(JOBS_PER_RUN)) {
            if (batchApplier.applyNextBatch(jobId)) {
                processedJobs++;
            }
        }
        return processedJobs;
    }
}

package br.com.certamecards.officialdeck.service;

import br.com.certamecards.common.config.OfficialProperties;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import br.com.certamecards.officialdeck.persistence.ContentUpdateStatesWriter;
import br.com.certamecards.officialdeck.persistence.ContentUpdateSubscribersQuery;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ContentUpdateBatchApplier {

    private static final Logger log = LoggerFactory.getLogger(ContentUpdateBatchApplier.class);
    private static final Duration SLOW_JOB = Duration.ofMinutes(5);

    private final ContentUpdateJobStore jobStore;
    private final ContentUpdateSubscribersQuery subscribersQuery;
    private final ContentUpdateStatesWriter statesWriter;
    private final OfficialProperties properties;
    private final Clock clock;
    private final OfficialMetrics metrics;

    public ContentUpdateBatchApplier(
            ContentUpdateJobStore jobStore,
            ContentUpdateSubscribersQuery subscribersQuery,
            ContentUpdateStatesWriter statesWriter,
            OfficialProperties properties,
            Clock clock,
            OfficialMetrics metrics) {
        this.jobStore = jobStore;
        this.subscribersQuery = subscribersQuery;
        this.statesWriter = statesWriter;
        this.properties = properties;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public boolean applyNextBatch(UUID jobId) {
        Optional<ContentUpdateJob> locked = jobStore.lockPending(jobId);
        if (locked.isEmpty()) {
            return false;
        }
        ContentUpdateJob job = locked.get();
        List<UUID> subscribers = subscribersQuery.lockNextBatch(job, properties.contentUpdateBatch());
        if (subscribers.isEmpty()) {
            jobStore.finish(job.id(), clock.instant());
            metrics.jobFinished(job.updatedAt(), clock.instant());
            log.info("Content update job {} finished", job.id());
            return true;
        }
        int applied = statesWriter.apply(job, subscribers, clock.instant());
        jobStore.advance(job.id(), subscribers.getLast(), applied);
        metrics.applied(applied);
        warnWhenSlow(job);
        return true;
    }

    private void warnWhenSlow(ContentUpdateJob job) {
        if (Duration.between(job.updatedAt(), clock.instant()).compareTo(SLOW_JOB) > 0) {
            log.warn("Content update job {} is running for more than {}", job.id(), SLOW_JOB);
        }
    }
}

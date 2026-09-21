package br.com.certamecards.officialdeck.service;

import br.com.certamecards.common.config.OfficialProperties;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import br.com.certamecards.officialdeck.persistence.ContentUpdateStatesWriter;
import br.com.certamecards.officialdeck.persistence.ContentUpdateSubscribersQuery;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ContentUpdateBatchApplier {

    private final ContentUpdateJobStore jobStore;
    private final ContentUpdateSubscribersQuery subscribersQuery;
    private final ContentUpdateStatesWriter statesWriter;
    private final OfficialProperties properties;
    private final Clock clock;

    public ContentUpdateBatchApplier(
            ContentUpdateJobStore jobStore,
            ContentUpdateSubscribersQuery subscribersQuery,
            ContentUpdateStatesWriter statesWriter,
            OfficialProperties properties,
            Clock clock) {
        this.jobStore = jobStore;
        this.subscribersQuery = subscribersQuery;
        this.statesWriter = statesWriter;
        this.properties = properties;
        this.clock = clock;
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
            return true;
        }
        int applied = statesWriter.apply(job, subscribers, clock.instant());
        jobStore.advance(job.id(), subscribers.getLast(), applied);
        return true;
    }
}

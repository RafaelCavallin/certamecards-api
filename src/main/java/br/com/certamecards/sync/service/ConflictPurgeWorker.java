package br.com.certamecards.sync.service;

import br.com.certamecards.sync.persistence.ConflictPurgeQuery;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConflictPurgeWorker {

    private static final String DAILY_AT_3_15AM = "0 15 3 * * *";

    private final ConflictPurgeQuery purgeQuery;
    private final Clock clock;

    public ConflictPurgeWorker(ConflictPurgeQuery purgeQuery, Clock clock) {
        this.purgeQuery = purgeQuery;
        this.clock = clock;
    }

    @Scheduled(cron = DAILY_AT_3_15AM)
    @Transactional
    public int expireDueConflicts() {
        return purgeQuery.expireDueConflicts(clock.instant());
    }
}

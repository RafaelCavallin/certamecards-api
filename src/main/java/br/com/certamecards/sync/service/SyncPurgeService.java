package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.SyncLimits;
import br.com.certamecards.sync.persistence.PurgeQuery;
import br.com.certamecards.sync.persistence.PurgeWatermarkQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncPurgeService {

    private static final Duration PURGE_WINDOW = Duration.ofDays(SyncLimits.PURGE_WINDOW_DAYS);
    private static final String DAILY_AT_3AM = "0 0 3 * * *";

    private final PurgeQuery purgeQuery;
    private final PurgeWatermarkQuery watermarkQuery;
    private final Clock clock;

    public SyncPurgeService(PurgeQuery purgeQuery, PurgeWatermarkQuery watermarkQuery, Clock clock) {
        this.purgeQuery = purgeQuery;
        this.watermarkQuery = watermarkQuery;
        this.clock = clock;
    }

    @Scheduled(cron = DAILY_AT_3AM)
    @Transactional
    public PurgeResult purgeDeletedRows() {
        Instant threshold = clock.instant().minus(PURGE_WINDOW);
        long maxChangeSeq = purgeQuery.maxPurgeableChangeSeq(threshold);
        int purgedCards = purgeQuery.purgeCards(threshold);
        int purgedDecks = purgeQuery.purgeDecks(threshold);
        if (maxChangeSeq > 0) {
            watermarkQuery.advanceWatermark(maxChangeSeq);
        }
        return new PurgeResult(purgedCards, purgedDecks);
    }
}

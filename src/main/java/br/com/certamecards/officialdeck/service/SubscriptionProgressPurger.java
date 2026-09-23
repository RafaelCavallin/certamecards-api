package br.com.certamecards.officialdeck.service;

import br.com.certamecards.officialdeck.domain.PurgeTarget;
import br.com.certamecards.officialdeck.persistence.ProgressPurgeWriter;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SubscriptionProgressPurger {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionProgressPurger.class);
    static final int WARN_CARDS_THRESHOLD = 10_000;

    private final ProgressPurgeWriter purgeWriter;
    private final Clock clock;
    private final OfficialMetrics metrics;

    public SubscriptionProgressPurger(ProgressPurgeWriter purgeWriter, Clock clock, OfficialMetrics metrics) {
        this.purgeWriter = purgeWriter;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public int purge(PurgeTarget target) {
        int resetCards = purgeWriter.reset(target, clock.instant());
        purgeWriter.markPurged(target, clock.instant());
        metrics.purgedCards(resetCards);
        log.info("Purged progress of {} cards from a cancelled subscription", resetCards);
        if (resetCards > WARN_CARDS_THRESHOLD) {
            log.warn("Progress purge reset {} cards in a single subscription", resetCards);
        }
        return resetCards;
    }
}

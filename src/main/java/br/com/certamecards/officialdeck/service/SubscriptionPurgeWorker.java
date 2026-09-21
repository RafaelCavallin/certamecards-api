package br.com.certamecards.officialdeck.service;

import br.com.certamecards.common.config.OfficialProperties;
import br.com.certamecards.officialdeck.domain.PurgeTarget;
import br.com.certamecards.officialdeck.domain.SubscriptionProgressPolicy;
import br.com.certamecards.officialdeck.persistence.PurgeableSubscriptionsQuery;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPurgeWorker {

    private static final String DAILY_AT_3_30AM = "0 30 3 * * *";

    private final PurgeableSubscriptionsQuery purgeableQuery;
    private final SubscriptionProgressPurger purger;
    private final SubscriptionProgressPolicy policy;
    private final OfficialProperties properties;
    private final Clock clock;

    public SubscriptionPurgeWorker(
            PurgeableSubscriptionsQuery purgeableQuery,
            SubscriptionProgressPurger purger,
            SubscriptionProgressPolicy policy,
            OfficialProperties properties,
            Clock clock) {
        this.purgeableQuery = purgeableQuery;
        this.purger = purger;
        this.policy = policy;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = DAILY_AT_3_30AM)
    public int runDailyPurge() {
        var threshold = policy.purgeThreshold(clock.instant());
        int purgedSubscriptions = 0;
        for (PurgeTarget target : purgeableQuery.find(threshold, properties.contentUpdateBatch())) {
            purger.purge(target);
            purgedSubscriptions++;
        }
        return purgedSubscriptions;
    }
}

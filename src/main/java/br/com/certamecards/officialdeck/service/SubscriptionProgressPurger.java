package br.com.certamecards.officialdeck.service;

import br.com.certamecards.officialdeck.domain.PurgeTarget;
import br.com.certamecards.officialdeck.persistence.ProgressPurgeWriter;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SubscriptionProgressPurger {

    private final ProgressPurgeWriter purgeWriter;
    private final Clock clock;

    public SubscriptionProgressPurger(ProgressPurgeWriter purgeWriter, Clock clock) {
        this.purgeWriter = purgeWriter;
        this.clock = clock;
    }

    @Transactional
    public int purge(PurgeTarget target) {
        int resetCards = purgeWriter.reset(target, clock.instant());
        purgeWriter.markPurged(target, clock.instant());
        return resetCards;
    }
}

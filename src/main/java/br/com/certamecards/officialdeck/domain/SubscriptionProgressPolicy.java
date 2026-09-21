package br.com.certamecards.officialdeck.domain;

import br.com.certamecards.common.config.LibraryProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionProgressPolicy {

    private final LibraryProperties properties;

    public SubscriptionProgressPolicy(LibraryProperties properties) {
        this.properties = properties;
    }

    public Instant purgeThreshold(Instant now) {
        return now.minus(Duration.ofDays(properties.progressRetentionDays()));
    }

    public boolean isExpired(Instant cancelledAt, Instant now) {
        return cancelledAt != null && cancelledAt.isBefore(purgeThreshold(now));
    }
}

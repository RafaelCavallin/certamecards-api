package br.com.certamecards.officialdeck.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.config.LibraryProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionProgressPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-19T14:00:00Z");

    private final SubscriptionProgressPolicy policy =
            new SubscriptionProgressPolicy(new LibraryProperties(90, 10, 5, 20, 500, 3));

    @Test
    void givenTU47_whenComputingThreshold_thenItIsNinetyDaysBeforeNow() {
        assertThat(policy.purgeThreshold(NOW)).isEqualTo(Instant.parse("2026-06-21T14:00:00Z"));
    }

    @Test
    void givenTU47_whenCancelledNinetyOneDaysAgo_thenIsExpired() {
        assertThat(policy.isExpired(NOW.minusSeconds(91L * 24 * 3600), NOW)).isTrue();
    }

    @Test
    void givenTU47_whenCancelledExactlyNinetyDaysAgo_thenIsNotExpiredYet() {
        assertThat(policy.isExpired(NOW.minusSeconds(90L * 24 * 3600), NOW)).isFalse();
    }

    @Test
    void givenTU47_whenCancelledThirtyDaysAgo_thenIsNotExpired() {
        assertThat(policy.isExpired(NOW.minusSeconds(30L * 24 * 3600), NOW)).isFalse();
    }

    @Test
    void givenTU47_whenSubscriptionIsActive_thenIsNotExpired() {
        assertThat(policy.isExpired(null, NOW)).isFalse();
    }
}

package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.config.OfficialProperties;
import br.com.certamecards.officialdeck.domain.PurgeTarget;
import br.com.certamecards.officialdeck.domain.SubscriptionProgressPolicy;
import br.com.certamecards.officialdeck.persistence.ProgressPurgeWriter;
import br.com.certamecards.officialdeck.persistence.PurgeableSubscriptionsQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPurgeWorkerTest {

    private static final Instant NOW = Instant.parse("2026-09-19T14:00:00Z");

    private final PurgeableSubscriptionsQuery query = mock(PurgeableSubscriptionsQuery.class);
    private final ProgressPurgeWriter writer = mock(ProgressPurgeWriter.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SubscriptionProgressPurger purger =
            new SubscriptionProgressPurger(writer, clock, new OfficialMetrics(registry));
    private final SubscriptionPurgeWorker worker = new SubscriptionPurgeWorker(
            query,
            purger,
            new SubscriptionProgressPolicy(new LibraryProperties(90, 10, 5, 20, 500, 3)),
            new OfficialProperties(500, Duration.ofSeconds(1)),
            clock);

    @Test
    void givenExpiredSubscriptions_whenRunningDailyPurge_thenResetsAndMarksEachOne() {
        PurgeTarget first = new PurgeTarget(UUID.randomUUID(), UUID.randomUUID());
        PurgeTarget second = new PurgeTarget(UUID.randomUUID(), UUID.randomUUID());
        when(query.find(Instant.parse("2026-06-21T14:00:00Z"), 500)).thenReturn(List.of(first, second));
        when(writer.reset(first, NOW)).thenReturn(3);

        assertThat(worker.runDailyPurge()).isEqualTo(2);

        verify(writer).markPurged(first, NOW);
        verify(writer).markPurged(second, NOW);
    }

    @Test
    void givenNothingExpired_whenRunningDailyPurge_thenReturnsZero() {
        when(query.find(Instant.parse("2026-06-21T14:00:00Z"), 500)).thenReturn(List.of());

        assertThat(worker.runDailyPurge()).isZero();
    }

    @Test
    void givenPurgedSubscription_whenRunningDailyPurge_thenCountsTheResetCards() {
        PurgeTarget target = new PurgeTarget(UUID.randomUUID(), UUID.randomUUID());
        when(query.find(Instant.parse("2026-06-21T14:00:00Z"), 500)).thenReturn(List.of(target));
        when(writer.reset(target, NOW)).thenReturn(12_000);

        worker.runDailyPurge();

        assertThat(registry.counter("subscription.purge.cards").count()).isEqualTo(12_000);
    }
}

package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.sync.persistence.PurgeQuery;
import br.com.certamecards.sync.persistence.PurgeWatermarkQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SyncPurgeServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final PurgeQuery purgeQuery = mock(PurgeQuery.class);
    private final PurgeWatermarkQuery watermarkQuery = mock(PurgeWatermarkQuery.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SyncPurgeService service = new SyncPurgeService(purgeQuery, watermarkQuery, clock);

    @Test
    void givenPurgeableRows_whenPurging_thenAdvancesWatermark() {
        when(purgeQuery.maxPurgeableChangeSeq(any())).thenReturn(42L);
        when(purgeQuery.purgeCards(any())).thenReturn(3);
        when(purgeQuery.purgeDecks(any())).thenReturn(1);

        PurgeResult result = service.purgeDeletedRows();

        assertThat(result.purgedCards()).isEqualTo(3);
        assertThat(result.purgedDecks()).isEqualTo(1);
        verify(watermarkQuery).advanceWatermark(42L);
    }

    @Test
    void givenNothingPurgeable_whenPurging_thenDoesNotAdvanceWatermark() {
        when(purgeQuery.maxPurgeableChangeSeq(any())).thenReturn(0L);
        when(purgeQuery.purgeCards(any())).thenReturn(0);
        when(purgeQuery.purgeDecks(any())).thenReturn(0);

        PurgeResult result = service.purgeDeletedRows();

        assertThat(result.purgedCards()).isZero();
        verify(watermarkQuery, never()).advanceWatermark(any(Long.class));
    }
}

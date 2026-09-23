package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.sync.domain.ChangeEntry;
import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.persistence.GlobalChangesQuery;
import br.com.certamecards.sync.persistence.PurgeWatermarkQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SyncChangesServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final GlobalChangesQuery globalChangesQuery = mock(GlobalChangesQuery.class);
    private final PurgeWatermarkQuery watermarkQuery = mock(PurgeWatermarkQuery.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SyncChangesService service =
            new SyncChangesService(globalChangesQuery, watermarkQuery, new SimpleMeterRegistry(), clock);

    @Test
    void givenNoChanges_whenFetching_thenNextCursorStaysAtRequestedCursor() {
        when(watermarkQuery.currentWatermark()).thenReturn(0L);
        when(globalChangesQuery.fetch(any(), anyLong(), anyInt(), any())).thenReturn(List.of());

        ChangesPage page = service.changesSince(USER_ID, 5L, 10);

        assertThat(page.changes()).isEmpty();
        assertThat(page.nextCursor()).isEqualTo(5L);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void givenChangesWithinLimit_whenFetching_thenNextCursorIsLastChangeSeq() {
        when(watermarkQuery.currentWatermark()).thenReturn(0L);
        ChangeEntry entry = new ChangeEntry(9L, "deck", null);
        when(globalChangesQuery.fetch(any(), anyLong(), anyInt(), any())).thenReturn(List.of(entry));

        ChangesPage page = service.changesSince(USER_ID, 0L, 10);

        assertThat(page.nextCursor()).isEqualTo(9L);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void givenCursorBeforeWatermark_whenFetching_thenThrowsResyncRequired() {
        when(watermarkQuery.currentWatermark()).thenReturn(100L);

        assertThatThrownBy(() -> service.changesSince(USER_ID, 5L, 10)).isInstanceOf(ApiException.class);
    }
}

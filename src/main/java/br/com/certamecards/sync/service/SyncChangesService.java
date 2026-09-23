package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.ChangeEntry;
import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.domain.SyncLimits;
import br.com.certamecards.sync.persistence.GlobalChangesQuery;
import br.com.certamecards.sync.persistence.PurgeWatermarkQuery;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyncChangesService {

    private static final String PAGE_ITEMS_METRIC = "sync.changes.page_items";

    private final GlobalChangesQuery globalChangesQuery;
    private final PurgeWatermarkQuery watermarkQuery;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public SyncChangesService(
            GlobalChangesQuery globalChangesQuery,
            PurgeWatermarkQuery watermarkQuery,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.globalChangesQuery = globalChangesQuery;
        this.watermarkQuery = watermarkQuery;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public ChangesPage changesSince(UUID userId, long cursor, int limit) {
        ensureCursorNotPurged(cursor);
        List<ChangeEntry> fetched = globalChangesQuery.fetch(userId, cursor, limit + 1, reviewLogWindowStart());
        boolean hasMore = fetched.size() > limit;
        List<ChangeEntry> page = hasMore ? fetched.subList(0, limit) : fetched;
        long nextCursor = page.isEmpty() ? cursor : page.get(page.size() - 1).changeSeq();
        meterRegistry.summary(PAGE_ITEMS_METRIC).record(page.size());
        return new ChangesPage(clock.instant(), page, nextCursor, hasMore);
    }

    private Instant reviewLogWindowStart() {
        return clock.instant().minus(Duration.ofDays(SyncLimits.REVIEW_LOG_WINDOW_DAYS));
    }

    private void ensureCursorNotPurged(long cursor) {
        if (cursor > 0 && cursor < watermarkQuery.currentWatermark()) {
            throw ApiException.of(ErrorCode.RESYNC_REQUIRED);
        }
    }
}

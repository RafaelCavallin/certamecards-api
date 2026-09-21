package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.domain.SyncLimits;
import br.com.certamecards.sync.persistence.CardStatesChangesQuery;
import br.com.certamecards.sync.persistence.CardsChangesQuery;
import br.com.certamecards.sync.persistence.DecksChangesQuery;
import br.com.certamecards.sync.persistence.PurgeWatermarkQuery;
import br.com.certamecards.sync.persistence.ReviewLogsChangesQuery;
import br.com.certamecards.sync.persistence.ReviewVoidsChangesQuery;
import br.com.certamecards.sync.persistence.SettingsChangesQuery;
import br.com.certamecards.sync.persistence.SubjectsChangesQuery;
import br.com.certamecards.sync.persistence.SubscriptionsChangesQuery;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SyncChangesService {

    private static final String PAGE_ITEMS_METRIC = "sync.changes.page_items";

    private final SubjectsChangesQuery subjectsQuery;
    private final DecksChangesQuery decksQuery;
    private final CardsChangesQuery cardsQuery;
    private final CardStatesChangesQuery cardStatesQuery;
    private final ReviewLogsChangesQuery reviewLogsQuery;
    private final ReviewVoidsChangesQuery reviewVoidsQuery;
    private final SubscriptionsChangesQuery subscriptionsQuery;
    private final SettingsChangesQuery settingsQuery;
    private final PurgeWatermarkQuery watermarkQuery;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public SyncChangesService(
            SubjectsChangesQuery subjectsQuery,
            DecksChangesQuery decksQuery,
            CardsChangesQuery cardsQuery,
            CardStatesChangesQuery cardStatesQuery,
            ReviewLogsChangesQuery reviewLogsQuery,
            ReviewVoidsChangesQuery reviewVoidsQuery,
            SubscriptionsChangesQuery subscriptionsQuery,
            SettingsChangesQuery settingsQuery,
            PurgeWatermarkQuery watermarkQuery,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.subjectsQuery = subjectsQuery;
        this.decksQuery = decksQuery;
        this.cardsQuery = cardsQuery;
        this.cardStatesQuery = cardStatesQuery;
        this.reviewLogsQuery = reviewLogsQuery;
        this.reviewVoidsQuery = reviewVoidsQuery;
        this.subscriptionsQuery = subscriptionsQuery;
        this.settingsQuery = settingsQuery;
        this.watermarkQuery = watermarkQuery;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public ChangesPage changesSince(UUID userId, long cursor, int limit) {
        ensureCursorNotPurged(cursor);
        ChangesPageBuilder builder = new ChangesPageBuilder(limit);
        builder.addSubjects(subjectsQuery.fetch(cursor, builder.remaining()));
        builder.addDecks(decksQuery.fetch(userId, cursor, builder.remaining()));
        builder.addCards(cardsQuery.fetch(userId, cursor, builder.remaining()));
        builder.addCardStates(cardStatesQuery.fetch(userId, cursor, builder.remaining()));
        builder.addReviewLogs(reviewLogsQuery.fetch(userId, cursor, builder.remaining(), reviewLogWindowStart()));
        builder.addReviewVoids(reviewVoidsQuery.fetch(userId, cursor, builder.remaining()));
        builder.addSubscriptions(subscriptionsQuery.fetch(userId, cursor, builder.remaining()));
        builder.setSettings(settingsQuery.fetch(userId, cursor));
        ChangesPage page = builder.build(cursor, limit);
        meterRegistry.summary(PAGE_ITEMS_METRIC).record(builder.itemCount());
        return page;
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

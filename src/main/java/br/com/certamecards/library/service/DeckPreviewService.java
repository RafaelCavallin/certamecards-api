package br.com.certamecards.library.service;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckPreview;
import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.persistence.LibraryDeckLookupQuery;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeckPreviewService {

    private final OfficialDeckAccess deckAccess;
    private final DeckAvailabilityGuard availabilityGuard;
    private final LibraryDeckLookupQuery lookupQuery;
    private final LibraryProperties properties;
    private final LibraryMetrics metrics;

    public DeckPreviewService(
            OfficialDeckAccess deckAccess,
            DeckAvailabilityGuard availabilityGuard,
            LibraryDeckLookupQuery lookupQuery,
            LibraryProperties properties,
            LibraryMetrics metrics) {
        this.deckAccess = deckAccess;
        this.availabilityGuard = availabilityGuard;
        this.lookupQuery = lookupQuery;
        this.properties = properties;
        this.metrics = metrics;
    }

    public DeckPreview preview(UUID userId, UUID deckId) {
        availabilityGuard.ensurePublished(deckAccess.findOfficial(deckId));
        LibraryDeckSummary summary =
                lookupQuery.findSummary(userId, deckId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        metrics.preview();
        return new DeckPreview(summary, lookupQuery.previewCards(deckId, properties.previewCards()));
    }
}

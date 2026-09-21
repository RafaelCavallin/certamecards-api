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

    public DeckPreviewService(
            OfficialDeckAccess deckAccess,
            DeckAvailabilityGuard availabilityGuard,
            LibraryDeckLookupQuery lookupQuery,
            LibraryProperties properties) {
        this.deckAccess = deckAccess;
        this.availabilityGuard = availabilityGuard;
        this.lookupQuery = lookupQuery;
        this.properties = properties;
    }

    public DeckPreview preview(UUID userId, UUID deckId) {
        availabilityGuard.ensurePublished(deckAccess.findOfficial(deckId));
        LibraryDeckSummary summary =
                lookupQuery.findSummary(userId, deckId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        return new DeckPreview(summary, lookupQuery.previewCards(deckId, properties.previewCards()));
    }
}

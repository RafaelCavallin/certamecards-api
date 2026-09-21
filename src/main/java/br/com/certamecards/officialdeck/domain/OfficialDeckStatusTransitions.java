package br.com.certamecards.officialdeck.domain;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckStatusTransitions {

    private final LibraryProperties libraryProperties;

    public OfficialDeckStatusTransitions(LibraryProperties libraryProperties) {
        this.libraryProperties = libraryProperties;
    }

    public void validate(
            OfficialDeckStatus from, OfficialDeckStatus to, int activeCardCount, int activeSubscriberCount) {
        if (from == OfficialDeckStatus.DRAFT && to == OfficialDeckStatus.PUBLISHED) {
            requireMinCards(activeCardCount);
            return;
        }
        if (from == OfficialDeckStatus.PUBLISHED && to == OfficialDeckStatus.DRAFT) {
            requireNoSubscribers(activeSubscriberCount);
            return;
        }
        if (from == OfficialDeckStatus.PUBLISHED && to == OfficialDeckStatus.DISCONTINUED) {
            return;
        }
        if (from == OfficialDeckStatus.DISCONTINUED && to == OfficialDeckStatus.PUBLISHED) {
            requireMinCards(activeCardCount);
            return;
        }
        throw ApiException.of(ErrorCode.VALIDATION_FAILED);
    }

    private void requireMinCards(int activeCardCount) {
        if (activeCardCount < libraryProperties.minCardsToPublish()) {
            throw ApiException.of(ErrorCode.OFFICIAL_DECK_MIN_CARDS);
        }
    }

    private void requireNoSubscribers(int activeSubscriberCount) {
        if (activeSubscriberCount > 0) {
            throw ApiException.of(ErrorCode.OFFICIAL_DECK_HAS_SUBSCRIBERS);
        }
    }
}

package br.com.certamecards.officialdeck.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class OfficialDeckStatusTransitionsTest {

    private final LibraryProperties libraryProperties = new LibraryProperties(90, 10, 5, 20, 500, 3);
    private final OfficialDeckStatusTransitions transitions = new OfficialDeckStatusTransitions(libraryProperties);

    @Test
    void givenFourActiveCards_whenPublishingFromDraft_thenThrowsMinCards() {
        assertThatThrownBy(() -> transitions.validate(OfficialDeckStatus.DRAFT, OfficialDeckStatus.PUBLISHED, 4, 0))
                .isInstanceOf(ApiException.class)
                .satisfies(ex ->
                        assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_DECK_MIN_CARDS));
    }

    @Test
    void givenFiveActiveCards_whenPublishingFromDraft_thenAccepted() {
        assertThatCode(() -> transitions.validate(OfficialDeckStatus.DRAFT, OfficialDeckStatus.PUBLISHED, 5, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void givenActiveSubscribers_whenUnpublishing_thenThrowsHasSubscribers() {
        assertThatThrownBy(() -> transitions.validate(OfficialDeckStatus.PUBLISHED, OfficialDeckStatus.DRAFT, 10, 1))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OFFICIAL_DECK_HAS_SUBSCRIBERS));
    }

    @Test
    void givenNoSubscribers_whenUnpublishing_thenAccepted() {
        assertThatCode(() -> transitions.validate(OfficialDeckStatus.PUBLISHED, OfficialDeckStatus.DRAFT, 10, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void givenPublished_whenDiscontinuing_thenAlwaysAccepted() {
        assertThatCode(() ->
                        transitions.validate(OfficialDeckStatus.PUBLISHED, OfficialDeckStatus.DISCONTINUED, 0, 999))
                .doesNotThrowAnyException();
    }

    @Test
    void givenDiscontinued_whenRepublishingWithoutEnoughCards_thenThrowsMinCards() {
        assertThatThrownBy(
                        () -> transitions.validate(OfficialDeckStatus.DISCONTINUED, OfficialDeckStatus.PUBLISHED, 2, 0))
                .isInstanceOf(ApiException.class)
                .satisfies(ex ->
                        assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.OFFICIAL_DECK_MIN_CARDS));
    }

    @Test
    void givenDiscontinued_whenMovingToDraft_thenThrowsValidationFailed() {
        assertThatThrownBy(() -> transitions.validate(OfficialDeckStatus.DISCONTINUED, OfficialDeckStatus.DRAFT, 10, 0))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void givenDraft_whenDiscontinuingDirectly_thenThrowsValidationFailed() {
        assertThatThrownBy(() -> transitions.validate(OfficialDeckStatus.DRAFT, OfficialDeckStatus.DISCONTINUED, 10, 0))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}

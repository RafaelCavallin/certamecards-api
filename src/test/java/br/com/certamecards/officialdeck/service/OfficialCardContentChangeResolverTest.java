package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import org.junit.jupiter.api.Test;

class OfficialCardContentChangeResolverTest {

    private final OfficialCardContentChangeResolver resolver = new OfficialCardContentChangeResolver();

    @Test
    void givenDraftDeck_whenContentChangedOmitted_thenDefaultsToFalse() {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("F", "B", null, null, null, 0);

        assertThat(resolver.resolve(OfficialDeckStatus.DRAFT, command)).isFalse();
    }

    @Test
    void givenPublishedDeck_whenContentChangedOmitted_thenThrowsValidationFailed() {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("F", "B", null, null, null, 0);

        assertThatThrownBy(() -> resolver.resolve(OfficialDeckStatus.PUBLISHED, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void givenPublishedDeckAndContentChangedTrueWithoutNote_thenThrowsFieldError() {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("F", "B", null, true, "  ", 0);

        assertThatThrownBy(() -> resolver.resolve(OfficialDeckStatus.PUBLISHED, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(apiException.getFields()).hasSize(1);
                    assertThat(apiException.getFields().get(0).field()).isEqualTo("note");
                });
    }

    @Test
    void givenPublishedDeckAndContentChangedTrueWithNote_thenReturnsTrue() {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("F", "B", null, true, "Motivo", 0);

        assertThat(resolver.resolve(OfficialDeckStatus.PUBLISHED, command)).isTrue();
    }

    @Test
    void givenDiscontinuedDeckAndContentChangedFalse_thenReturnsFalseWithoutRequiringNote() {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("F", "B", null, false, null, 0);

        assertThat(resolver.resolve(OfficialDeckStatus.DISCONTINUED, command)).isFalse();
    }
}

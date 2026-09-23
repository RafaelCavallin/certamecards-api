package br.com.certamecards.officialdeck.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import org.junit.jupiter.api.Test;

class OfficialDeckStatusParserTest {

    @Test
    void givenNullStatus_whenParsingOptional_thenReturnsNull() {
        assertThat(OfficialDeckStatusParser.parseOptional(null)).isNull();
    }

    @Test
    void givenKnownCode_whenParsingOptional_thenReturnsStatus() {
        assertThat(OfficialDeckStatusParser.parseOptional(OfficialDeckStatus.DRAFT.code()))
                .isEqualTo(OfficialDeckStatus.DRAFT);
    }

    @Test
    void givenUnknownCode_whenParsingRequired_thenRejectsAsValidationFailure() {
        assertThatThrownBy(() -> OfficialDeckStatusParser.parseRequired("arquivado"))
                .isInstanceOf(ApiException.class);
    }
}

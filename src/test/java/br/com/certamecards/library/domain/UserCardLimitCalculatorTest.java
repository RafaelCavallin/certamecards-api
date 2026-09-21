package br.com.certamecards.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.CardLimitException;
import br.com.certamecards.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class UserCardLimitCalculatorTest {

    private final UserCardLimitCalculator calculator = new UserCardLimitCalculator();

    @Test
    void givenTU46_whenUserUsesNothing_thenAllFiftyThousandAreAvailable() {
        assertThat(calculator.availableCards(0)).isEqualTo(50_000);
    }

    @Test
    void givenTU46_whenUserUsesPartOfTheLimit_thenAvailableIsTheRemainder() {
        assertThat(calculator.availableCards(49_990)).isEqualTo(10);
    }

    @Test
    void givenTU46_whenUserIsOverTheLimit_thenAvailableNeverGoesNegative() {
        assertThat(calculator.availableCards(50_001)).isZero();
    }

    @Test
    void givenTU46_whenDeckFitsExactly_thenNothingIsThrown() {
        assertThatCode(() -> calculator.ensureFits(49_970, 30)).doesNotThrowAnyException();
    }

    @Test
    void givenTU46_whenDeckDoesNotFit_thenThrowsUserCardLimitWithBothNumbers() {
        assertThatThrownBy(() -> calculator.ensureFits(49_990, 30))
                .isInstanceOfSatisfying(CardLimitException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_CARD_LIMIT);
                    assertThat(exception.getRequiredCards()).isEqualTo(30);
                    assertThat(exception.getAvailableCards()).isEqualTo(10);
                    assertThat(exception.getDetail())
                            .isEqualTo("Este deck tem 30 cartões e você só tem espaço para 10. "
                                    + "Exclua cartões ou cancele uma inscrição.");
                });
    }
}

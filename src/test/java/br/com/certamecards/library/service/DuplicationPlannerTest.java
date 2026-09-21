package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.CardLimitException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.library.domain.DuplicateDeckCommand;
import br.com.certamecards.library.domain.DuplicationOptions;
import br.com.certamecards.library.domain.DuplicationPlan;
import br.com.certamecards.library.domain.DuplicationSource;
import br.com.certamecards.library.domain.UserCardLimitCalculator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DuplicationPlannerTest {

    private final DuplicationPlanner planner = new DuplicationPlanner(new UserCardLimitCalculator());

    @Test
    void givenTU53_whenSubscribedAndCarryingProgress_thenCarriesStatesAndKeepsSubscription() {
        DuplicationPlan plan = planner.plan(command(true, false), source("published", 10, true), 100);

        assertThat(plan).isEqualTo(new DuplicationPlan(true, false));
    }

    @Test
    void givenTU53_whenSubscribedAndCancelling_thenPlansCancellation() {
        DuplicationPlan plan = planner.plan(command(true, true), source("published", 10, true), 100);

        assertThat(plan.cancelSubscription()).isTrue();
    }

    @Test
    void givenTU53_whenStartingFromScratch_thenDoesNotCarryStates() {
        DuplicationPlan plan = planner.plan(command(false, false), source("published", 10, true), 100);

        assertThat(plan.carryStates()).isFalse();
    }

    @Test
    void givenTU53_whenNotSubscribed_thenNothingToCarryAndNothingToCancel() {
        DuplicationPlan plan = planner.plan(command(true, true), source("published", 10, false), 100);

        assertThat(plan).isEqualTo(new DuplicationPlan(false, false));
    }

    @Test
    void givenTU53_whenDiscontinuedAndNotSubscribed_thenThrowsNotSubscribed() {
        assertThat(codeOf(() -> planner.plan(command(true, false), source("discontinued", 10, false), 0)))
                .isEqualTo(ErrorCode.NOT_SUBSCRIBED);
    }

    @Test
    void givenTU53_whenDiscontinuedAndSubscribed_thenIsAllowed() {
        assertThat(planner.plan(command(true, false), source("discontinued", 10, true), 0)
                        .carryStates())
                .isTrue();
    }

    @Test
    void givenTU53_whenDraft_thenThrowsDeckNotAvailable() {
        assertThat(codeOf(() -> planner.plan(command(true, false), source("draft", 10, false), 0)))
                .isEqualTo(ErrorCode.DECK_NOT_AVAILABLE);
    }

    @Test
    void givenTU53_whenDeckIsDeleted_thenThrowsNotFound() {
        DuplicationSource source = source("published", 10, false);
        source.deck().markDeleted(Instant.EPOCH);

        assertThat(codeOf(() -> planner.plan(command(true, false), source, 0))).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void givenTU53_whenCopyExceedsUserLimit_thenThrowsWithRequiredAndAvailable() {
        assertThatThrownBy(() -> planner.plan(command(true, false), source("published", 30, false), 49_990))
                .isInstanceOfSatisfying(CardLimitException.class, ex -> {
                    assertThat(ex.getRequiredCards()).isEqualTo(30);
                    assertThat(ex.getAvailableCards()).isEqualTo(10);
                });
    }

    @Test
    void givenTU53_whenCancellingFreesTheSubscriptionSpace_thenCopyFits() {
        DuplicationPlan plan = planner.plan(command(true, true), source("published", 30, true), 50_000);

        assertThat(plan.cancelSubscription()).isTrue();
    }

    @Test
    void givenTU53_whenDeckHasMoreThanFiveThousandCards_thenThrowsDeckCardLimit() {
        assertThat(codeOf(() -> planner.plan(command(true, false), source("published", 5_001, false), 0)))
                .isEqualTo(ErrorCode.DECK_CARD_LIMIT);
    }

    private ErrorCode codeOf(Runnable action) {
        try {
            action.run();
        } catch (ApiException exception) {
            return exception.getErrorCode();
        }
        throw new AssertionError("no exception");
    }

    private DuplicateDeckCommand command(boolean carry, boolean cancel) {
        return new DuplicateDeckCommand(
                UUID.randomUUID(), UUID.randomUUID(), new DuplicationOptions(UUID.randomUUID(), carry, cancel));
    }

    private DuplicationSource source(String status, int cardCount, boolean subscribed) {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().changeStatus(status);
        for (int index = 0; index < cardCount; index++) {
            deck.getOfficialMeta().incrementCardCount();
        }
        return new DuplicationSource(deck, subscribed);
    }
}

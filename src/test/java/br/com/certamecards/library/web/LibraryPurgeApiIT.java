package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.library.web.LibraryTestCatalog.DeckSpec;
import br.com.certamecards.officialdeck.service.SubscriptionPurgeWorker;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class LibraryPurgeApiIT extends LibraryApiITBase {

    @Autowired
    private SubscriptionPurgeWorker purgeWorker;

    @Test
    void givenTI38_whenCancelledForMoreThanNinetyDays_thenProgressIsResetNotDeletedAndHistoryIsKept() throws Exception {
        Fixture fixture = studiedAndCancelled(91);

        int purged = purgeWorker.runDailyPurge();

        assertThat(purged).isGreaterThanOrEqualTo(1);
        Map<String, Object> studied = progress.stateRow(fixture.userId, fixture.studiedCardId);
        assertThat(studied.get("state")).isEqualTo(0);
        assertThat(studied.get("reps")).isEqualTo(0);
        assertThat(studied.get("review_count")).isEqualTo(2);
        assertThat(progress.stateRow(fixture.userId, fixture.suspendedCardId).get("suspended"))
                .isEqualTo(false);
        assertThat(progress.logCount(fixture.userId, fixture.studiedCardId, "reset"))
                .isEqualTo(1);
        assertThat(progress.logCount(fixture.userId, fixture.suspendedCardId, "reset"))
                .isZero();
        assertThat(progress.logCount(fixture.userId, fixture.studiedCardId, "review"))
                .isEqualTo(1);
        assertThat(purgedAt(fixture)).isNotNull();
    }

    @Test
    void givenTI38_whenPurgingTwice_thenSecondRunDoesNothingAndResubscribingStartsFromScratch() throws Exception {
        Fixture fixture = studiedAndCancelled(91);
        purgeWorker.runDailyPurge();
        int resetLogs = progress.logCount(fixture.userId, fixture.studiedCardId, "reset");

        purgeWorker.runDailyPurge();
        JsonNode again =
                fixtures.json(fixtures.subscribe(fixture.token, fixture.deckId).andExpect(status().isCreated()));
        JsonNode content =
                fixtures.json(fixtures.read(fixture.token, "/api/library/decks/" + fixture.deckId + "/content"));

        assertThat(progress.logCount(fixture.userId, fixture.studiedCardId, "reset"))
                .isEqualTo(resetLogs);
        assertThat(again.get("restoredProgress").asBoolean()).isFalse();
        assertThat(purgedAt(fixture)).isNull();
        content.get("cardStates").forEach(state -> {
            assertThat(state.get("state").asInt()).isZero();
            assertThat(state.get("suspended").asBoolean()).isFalse();
        });
    }

    @Test
    void givenTI38_whenCancelledForLessThanNinetyDays_thenProgressIsKeptAndRestoredOnResubscription() throws Exception {
        Fixture fixture = studiedAndCancelled(30);

        purgeWorker.runDailyPurge();
        JsonNode again =
                fixtures.json(fixtures.subscribe(fixture.token, fixture.deckId).andExpect(status().isCreated()));

        assertThat(progress.stateRow(fixture.userId, fixture.studiedCardId).get("state"))
                .isEqualTo(2);
        assertThat(progress.logCount(fixture.userId, fixture.studiedCardId, "reset"))
                .isZero();
        assertThat(again.get("restoredProgress").asBoolean()).isTrue();
    }

    private Fixture studiedAndCancelled(int daysAgo) throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String token = accounts.candidate(email);
        String deckId = catalog.publishedDeck(admin, new DeckSpec("Português", "Deck " + UUID.randomUUID(), "x", 5));
        fixtures.subscribe(token, deckId).andExpect(status().isCreated());
        String studied = adminCards.cards(admin, deckId).get(0).get("id").asString();
        String suspended = adminCards.cards(admin, deckId).get(1).get("id").asString();
        progress.study(token, studied, Instant.now().minusSeconds(120), 1);
        progress.suspend(token, suspended);
        fixtures.cancel(token, deckId).andExpect(status().isNoContent());
        jdbcClient
                .sql("UPDATE deck_subscriptions SET cancelled_at = now() - make_interval(days => :days) "
                        + "WHERE deck_id = :deckId")
                .param("days", daysAgo)
                .param("deckId", UUID.fromString(deckId))
                .update();
        return new Fixture(token, deckId, studied, suspended, fixtures.userId(email));
    }

    private Object purgedAt(Fixture fixture) {
        return jdbcClient
                .sql("SELECT progress_purged_at FROM deck_subscriptions WHERE user_id = :u AND deck_id = :d")
                .param("u", fixture.userId)
                .param("d", UUID.fromString(fixture.deckId))
                .query()
                .singleRow()
                .get("progress_purged_at");
    }

    private record Fixture(String token, String deckId, String studiedCardId, String suspendedCardId, UUID userId) {}
}

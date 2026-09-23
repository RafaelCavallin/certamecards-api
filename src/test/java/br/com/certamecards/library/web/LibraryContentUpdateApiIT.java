package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.library.web.LibraryTestCatalog.DeckSpec;
import br.com.certamecards.officialdeck.service.ContentUpdateWorker;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class LibraryContentUpdateApiIT extends LibraryApiITBase {

    private static final String CORRECTION = "{\"back\":\"Nova redação\",\"contentChanged\":false}";
    private static final String CHANGE =
            "{\"back\":\"Novo prazo\",\"contentChanged\":true,\"note\":\"Lei alterou o prazo.\"}";

    @Autowired
    private ContentUpdateWorker worker;

    @Test
    void givenTI40_whenCorrectingText_thenNoOneProgressIsTouched() throws Exception {
        Setup setup = setup();

        adminCards
                .edit(setup.admin, setup.deckId, setup.cardId, CORRECTION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentUpdateQueued").value(false));
        drainWorker();

        Map<String, Object> state = progress.stateRow(setup.userId, setup.cardId);
        assertThat(state.get("state")).isEqualTo(2);
        assertThat(state.get("review_count")).isEqualTo(1);
        assertThat(state.get("content_update_note")).isNull();
        assertThat(pendingJobs()).isZero();
    }

    @Test
    void givenTI40_whenChangingContent_thenSubscribersWithProgressGoToRelearningInTheWorker() throws Exception {
        Setup setup = setup();
        Map<String, Object> before = progress.stateRow(setup.userId, setup.cardId);

        adminCards
                .edit(setup.admin, setup.deckId, setup.cardId, CHANGE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentUpdateQueued").value(true));

        assertThat(progress.stateRow(setup.userId, setup.cardId).get("state")).isEqualTo(2);
        assertThat(pendingJobs()).isEqualTo(1);
        drainWorker();
        Map<String, Object> after = progress.stateRow(setup.userId, setup.cardId);
        assertThat(after.get("state")).isEqualTo(3);
        assertThat(after.get("learning_steps")).isEqualTo(0);
        assertThat(after.get("stability")).isEqualTo(before.get("stability"));
        assertThat(after.get("difficulty")).isEqualTo(before.get("difficulty"));
        assertThat(after.get("reps")).isEqualTo(before.get("reps"));
        assertThat(after.get("lapses")).isEqualTo(before.get("lapses"));
        assertThat(after.get("review_count")).isEqualTo(2);
        assertThat(after.get("content_update_note")).isEqualTo("Lei alterou o prazo.");
        assertThat(after.get("due")).isEqualTo(after.get("content_updated_at"));
        assertThat(progress.logCount(setup.userId, setup.cardId, "content_update"))
                .isEqualTo(1);
        assertThat(pendingJobs()).isZero();
    }

    @Test
    void givenTI41_whenContentChanges_thenOnlyThoseWithProgressAreTouchedAndLaterSubscribersGetNoNotice()
            throws Exception {
        Setup setup = setup();
        String withoutProgressEmail = uniqueEmail("nop");
        String withoutProgress = accounts.candidate(withoutProgressEmail);
        fixtures.subscribe(withoutProgress, setup.deckId).andExpect(status().isCreated());
        String latecomerEmail = uniqueEmail("late");
        String latecomer = accounts.candidate(latecomerEmail);

        adminCards.edit(setup.admin, setup.deckId, setup.cardId, CHANGE).andExpect(status().isOk());
        drainWorker();
        fixtures.subscribe(latecomer, setup.deckId).andExpect(status().isCreated());

        assertThat(rowsFor(fixtures.userId(withoutProgressEmail), setup.cardId)).isZero();
        assertThat(rowsFor(fixtures.userId(latecomerEmail), setup.cardId)).isZero();
        JsonNode content = fixtures.json(fixtures.read(latecomer, "/api/library/decks/" + setup.deckId + "/content"));
        assertThat(content.get("cardStates")).isEmpty();
        assertThat(content.get("cards").get(0).get("back").asString()).isNotBlank();
    }

    @Test
    void givenTI41_whenContentUpdateIsApplied_thenLogIsNotARatingAndLaterReviewIsAcceptedAndClearsNotice()
            throws Exception {
        Setup setup = setup();
        adminCards.edit(setup.admin, setup.deckId, setup.cardId, CHANGE).andExpect(status().isOk());
        drainWorker();
        Instant editedAt = mutableClock.instant();

        JsonNode before = progress.study(setup.token, setup.cardId, editedAt.minusSeconds(5), 3);
        assertThat(before.get("appliedStates")).hasSize(1);
        assertThat(noteOf(setup)).isEqualTo("Lei alterou o prazo.");
        JsonNode after = progress.study(setup.token, setup.cardId, editedAt, 4);

        assertThat(after.get("appliedStates")).hasSize(1);
        assertThat(noteOf(setup)).isNull();
        assertThat(progress.stateRow(setup.userId, setup.cardId).get("content_updated_at"))
                .isNull();
        Object rating = jdbcClient
                .sql("SELECT rating FROM review_logs WHERE card_id = :cardId AND kind = 'content_update'")
                .param("cardId", UUID.fromString(setup.cardId))
                .query()
                .singleRow()
                .get("rating");
        assertThat(rating).isNull();
    }

    @Test
    void givenTI39_whenAdminDeletesOfficialCard_thenSubscribersLoseItButKeepHistoryAndStates() throws Exception {
        Setup setup = setup();

        adminCards.remove(setup.admin, setup.deckId, setup.cardId).andExpect(status().isNoContent());

        JsonNode content = fixtures.json(fixtures.read(setup.token, "/api/library/decks/" + setup.deckId + "/content"));
        assertThat(content.get("cards")).hasSize(4);
        assertThat(progress.logCount(setup.userId, setup.cardId, "review")).isEqualTo(1);
        assertThat(progress.stateRow(setup.userId, setup.cardId).get("state")).isEqualTo(2);
        JsonNode pull = fixtures.json(fixtures.read(setup.token, "/api/sync/changes?cursor=0"));
        assertThat(pull.get("changes").toString()).contains("\"deletedAt\":\"");
        JsonNode pushed =
                progress.study(setup.token, setup.cardId, Instant.now().minusSeconds(10), 2);
        assertThat(pushed.get("ignoredStates").get(0).get("reason").asString()).isEqualTo("card_deleted");
        assertThat(cardCountOf(setup.deckId)).isEqualTo(4);
    }

    private Setup setup() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String token = accounts.candidate(email);
        String deckId = catalog.publishedDeck(admin, new DeckSpec("Português", "Deck " + UUID.randomUUID(), "x", 5));
        fixtures.subscribe(token, deckId).andExpect(status().isCreated());
        String cardId = adminCards.firstCardId(admin, deckId);
        progress.study(token, cardId, Instant.now().minusSeconds(120), 1);
        return new Setup(admin, token, deckId, cardId, fixtures.userId(email));
    }

    private void drainWorker() {
        while (worker.runPendingBatch() > 0) {
            continue;
        }
    }

    private int pendingJobs() {
        return jdbcClient
                .sql("SELECT count(*) FROM official_content_update_jobs WHERE finished_at IS NULL")
                .query(Integer.class)
                .single();
    }

    private int rowsFor(UUID userId, String cardId) {
        return jdbcClient
                .sql("SELECT count(*) FROM card_states WHERE user_id = :u AND card_id = :c")
                .param("u", userId)
                .param("c", UUID.fromString(cardId))
                .query(Integer.class)
                .single();
    }

    private Object noteOf(Setup setup) {
        return progress.stateRow(setup.userId, setup.cardId).get("content_update_note");
    }

    private int cardCountOf(String deckId) {
        return jdbcClient
                .sql("SELECT card_count FROM decks WHERE id = :id")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
    }

    private record Setup(String admin, String token, String deckId, String cardId, UUID userId) {}
}

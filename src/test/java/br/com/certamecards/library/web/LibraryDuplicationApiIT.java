package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.library.web.LibraryTestCatalog.DeckSpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class LibraryDuplicationApiIT extends LibraryApiITBase {

    @Test
    void givenTI42_whenDuplicatingWithProgress_thenStatesAreInheritedWithASeedLogAndHistoryIsNotCopied()
            throws Exception {
        Fixture fixture = studiedSubscription();
        UUID copyId = UUID.randomUUID();

        JsonNode copy = fixtures.json(
                duplicate(fixture.token, fixture.deckId, copyId, "true", "true").andExpect(status().isCreated()));

        assertThat(copy.get("copiedCards").asInt()).isEqualTo(5);
        assertThat(copy.get("carriedStates").asInt()).isEqualTo(2);
        JsonNode deck = copy.get("deck");
        assertThat(deck.get("origin").asString()).isEqualTo("official_copy");
        assertThat(deck.get("originRef").asString()).isEqualTo(fixture.deckId);
        assertThat(deck.get("originLabel").asString()).isEqualTo(fixture.deckName);
        assertThat(deck.get("officialStatus").isNull()).isTrue();
        assertThat(cardIdsOf(copyId)).hasSize(5).doesNotContainAnyElementsOf(cardIdsOf(fixture.deckId));
        Map<String, Object> studiedCopy = progress.stateRow(fixture.userId, copyCardOf(copyId, "Frente 0"));
        assertThat(studiedCopy.get("state")).isEqualTo(2);
        assertThat(studiedCopy.get("review_count")).isEqualTo(1);
        Map<String, Object> suspendedCopy = progress.stateRow(fixture.userId, copyCardOf(copyId, "Frente 1"));
        assertThat(suspendedCopy.get("suspended")).isEqualTo(true);
        assertThat(suspendedCopy.get("review_count")).isEqualTo(0);
        assertThat(logsOfDeck(fixture.userId, copyId, "duplicate")).isEqualTo(1);
        assertThat(logsOfDeck(fixture.userId, copyId, "review")).isZero();
        assertThat(ratedLogsOfDeck(copyId)).isZero();
        assertThat(progress.stateRow(fixture.userId, fixture.studiedCardId).get("state"))
                .isEqualTo(2);
    }

    @Test
    void givenTI42_whenCancellingAtTheSameTime_thenSubscriptionEndsAndRepeatedIdReturnsTheSameCopy() throws Exception {
        Fixture fixture = studiedSubscription();
        UUID copyId = UUID.randomUUID();

        duplicate(fixture.token, fixture.deckId, copyId, "true", "true").andExpect(status().isCreated());
        ResultActions repeated = duplicate(fixture.token, fixture.deckId, copyId, "true", "true");

        repeated.andExpect(status().isOk()).andExpect(jsonPath("$.copiedCards").value(5));
        assertThat(cardIdsOf(copyId)).hasSize(5);
        assertThat(fixtures.subscriberCount(fixture.deckId)).isZero();
        fixtures.read(fixture.token, "/api/library/decks/" + fixture.deckId + "/content")
                .andExpect(status().isConflict());
        String copyCard = copyCardOf(copyId, "Frente 0");
        JsonNode pushed = progress.study(fixture.token, copyCard, Instant.now().minusSeconds(30), 2);
        assertThat(pushed.get("appliedStates")).hasSize(1);
    }

    @Test
    void givenTI43_whenDuplicatingFromScratch_thenNoStatesOrLogsAreCreatedAndSubscriptionRemains() throws Exception {
        Fixture fixture = studiedSubscription();
        UUID copyId = UUID.randomUUID();

        duplicate(fixture.token, fixture.deckId, copyId, "false", "false")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carriedStates").value(0));

        assertThat(statesOfDeck(fixture.userId, copyId)).isZero();
        assertThat(logsOfDeck(fixture.userId, copyId, "duplicate")).isZero();
        assertThat(fixtures.subscriberCount(fixture.deckId)).isEqualTo(1);
    }

    @Test
    void givenTI43_whenNotSubscribed_thenPublishedDeckIsCopiedWithoutStatesAndDiscontinuedOrDraftAreRefused()
            throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String token = accounts.candidate(uniqueEmail("cand"));
        String published = catalog.publishedDeck(admin, spec(5));
        String discontinued = catalog.publishedDeck(admin, spec(5));
        catalog.changeStatus(admin, discontinued, "discontinued");
        String draft = catalog.draftDeck(admin, spec(5));
        UUID copyId = UUID.randomUUID();

        duplicate(token, published, copyId, "true", "false")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carriedStates").value(0));
        duplicate(token, discontinued, UUID.randomUUID(), "true", "false")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("not_subscribed"));
        duplicate(token, draft, UUID.randomUUID(), "true", "false")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("deck_not_available"));
        duplicate(token, UUID.randomUUID().toString(), UUID.randomUUID(), "true", "false")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenTI43_whenSubscriberDuplicatesDiscontinuedDeck_thenItIsAllowed() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String token = accounts.candidate(uniqueEmail("cand"));
        String deckId = catalog.publishedDeck(admin, spec(5));
        fixtures.subscribe(token, deckId).andExpect(status().isCreated());
        catalog.changeStatus(admin, deckId, "discontinued");

        duplicate(token, deckId, UUID.randomUUID(), "true", "true").andExpect(status().isCreated());
    }

    @Test
    void givenCA16_whenCopyDoesNotFit_thenNothingIsCreatedAndCancellingFreesTheSubscriptionSpace() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String token = accounts.candidate(email);
        fixtures.fabricateOwnCards(fixtures.userId(email), 49_990);
        String deckId = catalog.publishedDeck(admin, spec(10));
        fixtures.subscribe(token, deckId).andExpect(status().isCreated());
        UUID refused = UUID.randomUUID();

        duplicate(token, deckId, refused, "true", "false")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user_card_limit"))
                .andExpect(jsonPath("$.requiredCards").value(10))
                .andExpect(jsonPath("$.availableCards").value(0));

        assertThat(cardIdsOf(refused)).isEmpty();
        assertThat(fixtures.subscriberCount(deckId)).isEqualTo(1);
        duplicate(token, deckId, UUID.randomUUID(), "true", "true").andExpect(status().isCreated());
    }

    @Test
    void givenDeckWithFiveThousandCards_whenDuplicating_thenEveryCardGetsADistinctId() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String token = accounts.candidate(uniqueEmail("cand"));
        String deckId = catalog.publishedDeck(admin, spec(5));
        jdbcClient
                .sql("INSERT INTO cards (id, deck_id, front, back) "
                        + "SELECT gen_random_uuid(), :deckId, 'f', 'b' FROM generate_series(1, 4995)")
                .param("deckId", UUID.fromString(deckId))
                .update();
        jdbcClient
                .sql("UPDATE decks SET card_count = 5000 WHERE id = :id")
                .param("id", UUID.fromString(deckId))
                .update();
        UUID copyId = UUID.randomUUID();

        duplicate(token, deckId, copyId, "false", "false")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.copiedCards").value(5000));

        Integer distinct = jdbcClient
                .sql("SELECT count(DISTINCT id) FROM cards WHERE deck_id = :id")
                .param("id", copyId)
                .query(Integer.class)
                .single();
        assertThat(distinct).isEqualTo(5000);
    }

    private Fixture studiedSubscription() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String token = accounts.candidate(email);
        DeckSpec spec = spec(5);
        String deckId = catalog.publishedDeck(admin, spec);
        fixtures.subscribe(token, deckId).andExpect(status().isCreated());
        String studied = cardIdWithFront(deckId, "Frente 0");
        progress.study(token, studied, Instant.now().minusSeconds(120), 1);
        progress.suspend(token, cardIdWithFront(deckId, "Frente 1"));
        return new Fixture(token, deckId, spec.name(), studied, fixtures.userId(email));
    }

    private ResultActions duplicate(String token, String deckId, UUID copyId, String carry, String cancel)
            throws Exception {
        return mockMvc.perform(post("/api/library/decks/" + deckId + "/duplicate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"" + copyId + "\",\"carryProgress\":" + carry + ",\"cancelSubscription\":" + cancel
                        + "}"));
    }

    private DeckSpec spec(int cards) {
        return new DeckSpec("Português", "Oficial " + UUID.randomUUID(), "Descrição", cards);
    }

    private String cardIdWithFront(String deckId, String front) {
        return jdbcClient
                .sql("SELECT id FROM cards WHERE deck_id = :deckId AND front = :front")
                .param("deckId", UUID.fromString(deckId))
                .param("front", front)
                .query(UUID.class)
                .single()
                .toString();
    }

    private String copyCardOf(UUID copyId, String front) {
        return cardIdWithFront(copyId.toString(), front);
    }

    private List<UUID> cardIdsOf(Object deckId) {
        UUID id = deckId instanceof UUID uuid ? uuid : UUID.fromString(deckId.toString());
        return jdbcClient
                .sql("SELECT id FROM cards WHERE deck_id = :id AND deleted_at IS NULL")
                .param("id", id)
                .query(UUID.class)
                .list();
    }

    private int logsOfDeck(UUID userId, UUID deckId, String kind) {
        return jdbcClient
                .sql("SELECT count(*) FROM review_logs rl JOIN cards c ON c.id = rl.card_id "
                        + "WHERE rl.user_id = :userId AND c.deck_id = :deckId AND rl.kind = :kind")
                .param("userId", userId)
                .param("deckId", deckId)
                .param("kind", kind)
                .query(Integer.class)
                .single();
    }

    private int ratedLogsOfDeck(UUID deckId) {
        return jdbcClient
                .sql("SELECT count(*) FROM review_logs rl JOIN cards c ON c.id = rl.card_id "
                        + "WHERE c.deck_id = :deckId AND rl.rating IS NOT NULL")
                .param("deckId", deckId)
                .query(Integer.class)
                .single();
    }

    private int statesOfDeck(UUID userId, UUID deckId) {
        return jdbcClient
                .sql("SELECT count(*) FROM card_states cs JOIN cards c ON c.id = cs.card_id "
                        + "WHERE cs.user_id = :userId AND c.deck_id = :deckId")
                .param("userId", userId)
                .param("deckId", deckId)
                .query(Integer.class)
                .single();
    }

    private record Fixture(String token, String deckId, String deckName, String studiedCardId, UUID userId) {}
}

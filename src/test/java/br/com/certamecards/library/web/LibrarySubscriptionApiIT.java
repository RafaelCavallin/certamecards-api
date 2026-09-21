package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.library.web.LibraryTestCatalog.DeckSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class LibrarySubscriptionApiIT extends LibraryApiITBase {

    @Test
    void givenTI33_whenSubscribing_thenDeckIsDeliveredWithoutCopyingContentAndCountsSubscribers() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String deckId = publishedDeck(admin, 7);

        JsonNode subscribed =
                fixtures.json(fixtures.subscribe(candidate, deckId).andExpect(status().isCreated()));

        assertThat(subscribed.get("deck").get("id").asString()).isEqualTo(deckId);
        assertThat(subscribed.get("deck").get("officialStatus").asString()).isEqualTo("published");
        assertThat(subscribed.get("deck").get("cardCount").asInt()).isEqualTo(7);
        assertThat(subscribed.get("subscription").get("cancelledAt").isNull()).isTrue();
        assertThat(subscribed.get("restoredProgress").asBoolean()).isFalse();
        assertThat(fixtures.subscriberCount(deckId)).isEqualTo(1);
        assertThat(cardsInDeckOwnedByUsers(deckId)).isZero();
        fixtures.subscribe(candidate, deckId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_subscribed"));
        assertThat(fixtures.subscriberCount(deckId)).isEqualTo(1);
    }

    @Test
    void givenTI33_whenSubscribing_thenContentIsPagedByIdAndPullCarriesTheSubscription() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String deckId = publishedDeck(admin, 7);
        fixtures.subscribe(candidate, deckId).andExpect(status().isCreated());

        JsonNode first = fixtures.json(fixtures.read(candidate, "/api/library/decks/" + deckId + "/content?limit=5"));
        String after = first.get("nextAfter").asString();
        JsonNode second = fixtures.json(
                fixtures.read(candidate, "/api/library/decks/" + deckId + "/content?limit=5&after=" + after));
        JsonNode pull = fixtures.json(fixtures.read(candidate, "/api/sync/changes?cursor=0"));

        assertThat(first.get("cards")).hasSize(5);
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(second.get("cards")).hasSize(2);
        assertThat(second.get("hasMore").asBoolean()).isFalse();
        assertThat(second.get("nextAfter").isNull()).isTrue();
        assertThat(ids(first, "cards")).doesNotContainAnyElementsOf(ids(second, "cards"));
        assertThat(pull.get("subscriptions").get(0).get("deckId").asString()).isEqualTo(deckId);
        assertThat(pull.get("cards")).hasSize(7);
    }

    @Test
    void givenTI33_whenNotSubscribedOrNotAvailable_thenSubscribingAndReadingContentAreRejected() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String draft = catalog.draftDeck(admin, spec("Rascunho", 5));
        String published = publishedDeck(admin, 5);

        fixtures.subscribe(candidate, draft)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("deck_not_available"));
        fixtures.subscribe(candidate, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        fixtures.read(candidate, "/api/library/decks/" + published + "/content")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("not_subscribed"));
        fixtures.cancel(candidate, published)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("not_subscribed"));
        fixtures.read(candidate, "/api/library/decks/" + published + "/content?limit=1001")
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenTI33_whenCancellingAndResubscribing_thenProgressIsKeptAndRestored() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String deckId = publishedDeck(admin, 5);
        fixtures.subscribe(candidate, deckId).andExpect(status().isCreated());
        String cardId = ids(
                        fixtures.json(fixtures.read(candidate, "/api/library/decks/" + deckId + "/content")), "cards")
                .getFirst();
        suspend(candidate, cardId);

        fixtures.cancel(candidate, deckId).andExpect(status().isNoContent());

        assertThat(fixtures.subscriberCount(deckId)).isZero();
        assertThat(storedStates(cardId)).isEqualTo(1);
        assertThat(cardsInDeck(deckId)).isEqualTo(5);
        fixtures.read(candidate, "/api/library/decks/" + deckId + "/content").andExpect(status().isConflict());
        JsonNode again = fixtures.json(fixtures.subscribe(candidate, deckId).andExpect(status().isCreated()));
        JsonNode content = fixtures.json(fixtures.read(candidate, "/api/library/decks/" + deckId + "/content"));
        assertThat(again.get("restoredProgress").asBoolean()).isTrue();
        assertThat(content.get("cardStates")).hasSize(1);
        assertThat(content.get("cardStates").get(0).get("suspended").asBoolean())
                .isTrue();
        assertThat(fixtures.subscriberCount(deckId)).isEqualTo(1);
    }

    @Test
    void givenTI34_whenDeckDoesNotFitTheFiftyThousandLimit_thenSubscriptionIsRejectedAndNothingIsCreated()
            throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String candidate = accounts.candidate(email);
        fixtures.fabricateOwnCards(fixtures.userId(email), 49_995);
        String tooBig = publishedDeck(admin, 12);
        String fits = publishedDeck(admin, 5);

        fixtures.subscribe(candidate, tooBig)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user_card_limit"))
                .andExpect(jsonPath("$.requiredCards").value(12))
                .andExpect(jsonPath("$.availableCards").value(5));

        assertThat(fixtures.subscriberCount(tooBig)).isZero();
        assertThat(activeSubscriptions(tooBig)).isZero();
        fixtures.subscribe(candidate, fits).andExpect(status().isCreated());
        fixtures.subscribe(candidate, tooBig)
                .andExpect(jsonPath("$.availableCards").value(0));
    }

    @Test
    void givenTI35_whenNewCardArrivesInSubscribedDeckAtTheLimit_thenItIsDeliveredIgnoringTheLimit() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String email = uniqueEmail("cand");
        String candidate = accounts.candidate(email);
        fixtures.fabricateOwnCards(fixtures.userId(email), 49_995);
        String deckId = publishedDeck(admin, 5);
        fixtures.subscribe(candidate, deckId).andExpect(status().isCreated());

        catalog.addCard(admin, deckId, "Cartão novo");

        JsonNode content = fixtures.json(fixtures.read(candidate, "/api/library/decks/" + deckId + "/content"));
        assertThat(content.get("cards")).hasSize(6);
        long cursor = lastOwnCardChangeSeq(email);
        JsonNode pull = fixtures.json(fixtures.read(candidate, "/api/sync/changes?cursor=" + cursor + "&limit=1000"));
        assertThat(ids(pull, "cards")).containsExactlyInAnyOrderElementsOf(ids(content, "cards"));
    }

    @Test
    void givenTI37_whenOneSubscriberSuspendsAnOfficialCard_thenOthersAreUnaffected() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String suspender = accounts.candidate(uniqueEmail("susp"));
        String other = accounts.candidate(uniqueEmail("other"));
        String outsider = accounts.candidate(uniqueEmail("out"));
        String deckId = publishedDeck(admin, 5);
        fixtures.subscribe(suspender, deckId).andExpect(status().isCreated());
        fixtures.subscribe(other, deckId).andExpect(status().isCreated());
        String cardId = ids(fixtures.json(fixtures.read(other, "/api/library/decks/" + deckId + "/content")), "cards")
                .getFirst();

        suspend(suspender, cardId);

        JsonNode suspenderPull = fixtures.json(fixtures.read(suspender, "/api/sync/changes?cursor=0"));
        JsonNode otherPull = fixtures.json(fixtures.read(other, "/api/sync/changes?cursor=0"));
        assertThat(suspenderPull.get("cardStates")).hasSize(1);
        assertThat(suspenderPull.get("cardStates").get(0).get("suspended").asBoolean())
                .isTrue();
        assertThat(otherPull.get("cardStates")).isEmpty();
        mockMvc.perform(put("/api/cards/" + cardId + "/suspension")
                        .header("Authorization", "Bearer " + outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspended\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenTI44_whenDeckIsDiscontinued_thenSubscribersKeepItAndNewSubscriptionsAreRefused() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String subscriber = accounts.candidate(uniqueEmail("sub"));
        String newcomer = accounts.candidate(uniqueEmail("new"));
        String deckId = publishedDeck(admin, 5);
        fixtures.subscribe(subscriber, deckId).andExpect(status().isCreated());

        catalog.changeStatus(admin, deckId, "discontinued");

        fixtures.subscribe(newcomer, deckId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("deck_not_available"));
        fixtures.read(newcomer, "/api/library/decks/" + deckId + "/preview")
                .andExpect(status().isUnprocessableEntity());
        JsonNode pull = fixtures.json(fixtures.read(subscriber, "/api/sync/changes?cursor=0"));
        assertThat(pull.get("decks").get(0).get("officialStatus").asString()).isEqualTo("discontinued");
        assertThat(pull.get("cards")).hasSize(5);
        fixtures.read(subscriber, "/api/library/decks/" + deckId + "/content").andExpect(status().isOk());
        fixtures.cancel(subscriber, deckId).andExpect(status().isNoContent());
    }

    private String publishedDeck(String admin, int cards) throws Exception {
        return catalog.publishedDeck(admin, spec("Deck", cards));
    }

    private DeckSpec spec(String prefix, int cards) {
        return new DeckSpec("Português", prefix + " " + UUID.randomUUID(), "Descrição", cards);
    }

    private void suspend(String token, String cardId) throws Exception {
        mockMvc.perform(put("/api/cards/" + cardId + "/suspension")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspended\":true}"))
                .andExpect(status().isOk());
    }

    private List<String> ids(JsonNode node, String field) {
        List<String> ids = new ArrayList<>();
        node.get(field).forEach(item -> ids.add(item.get("id").asString()));
        return ids;
    }

    private long lastOwnCardChangeSeq(String email) {
        return jdbcClient
                .sql("SELECT max(c.change_seq) FROM cards c JOIN decks d ON d.id = c.deck_id "
                        + "JOIN users u ON u.id = d.owner_id WHERE u.email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
    }

    private int storedStates(String cardId) {
        return jdbcClient
                .sql("SELECT count(*) FROM card_states WHERE card_id = :id")
                .param("id", UUID.fromString(cardId))
                .query(Integer.class)
                .single();
    }

    private int cardsInDeck(String deckId) {
        return jdbcClient
                .sql("SELECT count(*) FROM cards WHERE deck_id = :id AND deleted_at IS NULL")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
    }

    private int cardsInDeckOwnedByUsers(String deckId) {
        return jdbcClient
                .sql("SELECT count(*) FROM cards c JOIN decks d ON d.id = c.deck_id "
                        + "WHERE d.owner_id IS NOT NULL AND d.origin_ref = :id")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
    }

    private int activeSubscriptions(String deckId) {
        return jdbcClient
                .sql("SELECT count(*) FROM deck_subscriptions WHERE deck_id = :id AND cancelled_at IS NULL")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
    }
}

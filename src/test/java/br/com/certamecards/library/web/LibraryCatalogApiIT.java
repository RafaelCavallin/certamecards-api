package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.library.web.LibraryTestCatalog.DeckSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class LibraryCatalogApiIT extends LibraryApiITBase {

    @Test
    void givenTI31_whenSearching_thenOnlyPublishedDecksAppearWithAccentInsensitiveMatch() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String tag = "Ti31" + UUID.randomUUID().toString().substring(0, 8);
        String published = catalog.publishedDeck(
                admin, new DeckSpec("Português", "Regência " + tag, "Casos obrigatórios " + tag, 5));
        String draft = catalog.draftDeck(admin, new DeckSpec("Português", "Rascunho Regência " + tag, "x", 5));
        String discontinued = catalog.publishedDeck(admin, new DeckSpec("Português", "Antigo Regência " + tag, "x", 5));
        catalog.changeStatus(admin, discontinued, "discontinued");

        JsonNode byName = fixtures.json(fixtures.read(candidate, "/api/library/decks?q=REGENCIA " + tag.toUpperCase()));
        JsonNode byDescription = fixtures.json(fixtures.read(candidate, "/api/library/decks?q=obrigatorios " + tag));

        assertThat(ids(byName)).containsExactly(published).doesNotContain(draft, discontinued);
        assertThat(byName.get("total").asInt()).isEqualTo(1);
        JsonNode item = byName.get("items").get(0);
        assertThat(item.get("subjectName").asString()).isEqualTo("Português");
        assertThat(item.get("cardCount").asInt()).isEqualTo(5);
        assertThat(item.get("subscribed").asBoolean()).isFalse();
        assertThat(item.get("contentUpdatedAt").isNull()).isFalse();
        assertThat(item.get("description").asString()).isEqualTo("Casos obrigatórios " + tag);
        assertThat(ids(byDescription)).containsExactly(published);
    }

    @Test
    void givenTI31_whenFilteringBySubjectOrSearchingNothing_thenResultsAreScoped() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String tag = "Ti31b" + UUID.randomUUID().toString().substring(0, 8);
        String penal = catalog.publishedDeck(admin, new DeckSpec("Direito Penal", "Penal " + tag, "x", 5));
        catalog.publishedDeck(admin, new DeckSpec("Informática", "Redes " + tag, "x", 5));
        UUID penalSubject = catalog.subjectId("Direito Penal");

        JsonNode scoped =
                fixtures.json(fixtures.read(candidate, "/api/library/decks?q=" + tag + "&subjectId=" + penalSubject));
        JsonNode empty = fixtures.json(fixtures.read(candidate, "/api/library/decks?q=zzzzzz" + tag));

        assertThat(ids(scoped)).containsExactly(penal);
        assertThat(empty.get("items")).isEmpty();
        assertThat(empty.get("total").asInt()).isZero();
    }

    @Test
    void givenTI31_whenListing_thenOrderIsBySubjectThenName() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String tag = "Ti31c" + UUID.randomUUID().toString().substring(0, 8);
        String portugueseB = catalog.publishedDeck(admin, new DeckSpec("Português", "B " + tag, "x", 5));
        String portugueseA = catalog.publishedDeck(admin, new DeckSpec("Português", "A " + tag, "x", 5));
        String penal = catalog.publishedDeck(admin, new DeckSpec("Direito Penal", "Z " + tag, "x", 5));

        JsonNode result = fixtures.json(fixtures.read(candidate, "/api/library/decks?q=" + tag));

        assertThat(ids(result)).containsExactly(penal, portugueseA, portugueseB);
    }

    @Test
    void givenTI31_whenParametersAreInvalid_thenReturnsValidationFailed() throws Exception {
        String candidate = accounts.candidate(uniqueEmail("cand"));

        fixtures.read(candidate, "/api/library/decks?size=51")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        fixtures.read(candidate, "/api/library/decks?page=-1").andExpect(status().isBadRequest());
        fixtures.read(candidate, "/api/library/decks?q=" + "a".repeat(121)).andExpect(status().isBadRequest());
    }

    @Test
    void givenTI31_whenListingSubjects_thenOnlySubjectsWithPublishedDecksAppear() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String draftOnlySubject = "So rascunho " + suffix;
        insertSubject(draftOnlySubject);
        catalog.draftDeck(admin, new DeckSpec(draftOnlySubject, "Rascunho " + suffix, "x", 5));
        catalog.publishedDeck(admin, new DeckSpec("Informática", "Publicado " + suffix, "x", 5));

        JsonNode subjects = fixtures.json(fixtures.read(candidate, "/api/library/subjects"));

        List<String> names = new ArrayList<>();
        subjects.get("items").forEach(item -> names.add(item.get("name").asString()));
        assertThat(names).contains("Informática").doesNotContain(draftOnlySubject);
    }

    @Test
    void givenTI32_whenPreviewing_thenReturnsFirstTenActiveCardsInIdOrder() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String deckId = catalog.publishedDeck(admin, new DeckSpec("Português", "Prévia " + UUID.randomUUID(), "x", 12));

        JsonNode preview = fixtures.json(fixtures.read(candidate, "/api/library/decks/" + deckId + "/preview"));

        assertThat(preview.get("deck").get("id").asString()).isEqualTo(deckId);
        assertThat(preview.get("deck").get("cardCount").asInt()).isEqualTo(12);
        assertThat(preview.get("cards")).hasSize(10);
        assertThat(preview.get("cards").get(0).get("back").asString()).isEqualTo("Verso");
        assertThat(preview.get("cards").get(0).get("source").asString()).isEqualTo("Fonte");
        List<String> cardIds = new ArrayList<>();
        preview.get("cards").forEach(card -> cardIds.add(card.get("id").asString()));
        assertThat(cardIds).isSorted();
    }

    @Test
    void givenTI32_whenDeckIsNotPublishedOrNotOfficial_thenPreviewIsRejected() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String draft = catalog.draftDeck(admin, new DeckSpec("Português", "Rascunho " + UUID.randomUUID(), "x", 5));
        String ownDeck = ownDeckOf(candidate);

        fixtures.read(candidate, "/api/library/decks/" + draft + "/preview")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("deck_not_available"));
        fixtures.read(candidate, "/api/library/decks/" + ownDeck + "/preview").andExpect(status().isNotFound());
        fixtures.read(candidate, "/api/library/decks/" + UUID.randomUUID() + "/preview")
                .andExpect(status().isNotFound());
    }

    @Test
    void givenTI51_whenAskingForSuggestions_thenOneDeckPerSubjectAndSubscribedOnesAreIgnored() throws Exception {
        String admin = accounts.admin(uniqueEmail("admin"));
        String candidate = accounts.candidate(uniqueEmail("cand"));
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String subscribed = catalog.publishedDeck(admin, new DeckSpec("Português", "Sugestão P " + tag, "x", 20));
        String constitutional =
                catalog.publishedDeck(admin, new DeckSpec("Direito Constitucional", "Sugestão C " + tag, "x", 20));
        String penal = catalog.publishedDeck(admin, new DeckSpec("Direito Penal", "Sugestão D " + tag, "x", 20));
        String informatics = catalog.publishedDeck(admin, new DeckSpec("Informática", "Sugestão I " + tag, "x", 20));
        fixtures.subscribe(candidate, subscribed).andExpect(status().isCreated());

        JsonNode suggestions = fixtures.json(fixtures.read(candidate, "/api/library/suggestions?limit=3"));
        JsonNode byDefault = fixtures.json(fixtures.read(candidate, "/api/library/suggestions"));

        assertThat(ids(suggestions)).hasSize(3).doesNotContain(subscribed);
        assertThat(ids(suggestions)).containsExactlyInAnyOrder(constitutional, penal, informatics);
        assertThat(subjects(suggestions)).doesNotHaveDuplicates();
        assertThat(ids(byDefault)).hasSize(3);
        fixtures.read(candidate, "/api/library/suggestions?limit=6").andExpect(status().isBadRequest());
    }

    private List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.get("items").forEach(item -> ids.add(item.get("id").asString()));
        return ids;
    }

    private List<String> subjects(JsonNode page) {
        List<String> subjects = new ArrayList<>();
        page.get("items").forEach(item -> subjects.add(item.get("subjectId").asString()));
        return subjects;
    }

    private void insertSubject(String name) {
        jdbcClient
                .sql("INSERT INTO subjects (name, normalized_name) VALUES (:name, :normalized)")
                .param("name", name)
                .param("normalized", name.toLowerCase())
                .update();
    }

    private String ownDeckOf(String token) throws Exception {
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + catalog.subjectId("Português")
                                + "\",\"name\":\"Meu deck\"}"))
                .andExpect(status().isCreated());
        return deckId.toString();
    }
}

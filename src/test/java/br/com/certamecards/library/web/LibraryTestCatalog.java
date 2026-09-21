package br.com.certamecards.library.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class LibraryTestCatalog {

    private final LibraryTestBeans beans;

    LibraryTestCatalog(LibraryTestBeans beans) {
        this.beans = beans;
    }

    UUID subjectId(String subjectName) {
        return beans.subjects().findAll().stream()
                .filter(subject -> subject.getName().equals(subjectName))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    String draftDeck(String adminToken, DeckSpec spec) throws Exception {
        UUID deckId = UUID.randomUUID();
        beans.mockMvc()
                .perform(post("/api/admin/official-decks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId(spec.subject())
                                + "\",\"name\":\"" + spec.name() + "\",\"description\":\"" + spec.description()
                                + "\"}"))
                .andExpect(status().isCreated());
        for (int index = 0; index < spec.cards(); index++) {
            addCard(adminToken, deckId.toString(), "Frente " + index);
        }
        return deckId.toString();
    }

    String publishedDeck(String adminToken, DeckSpec spec) throws Exception {
        String deckId = draftDeck(adminToken, spec);
        changeStatus(adminToken, deckId, "published");
        return deckId;
    }

    void addCard(String adminToken, String deckId, String front) throws Exception {
        beans.mockMvc()
                .perform(post("/api/admin/official-decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"basic\",\"front\":\"" + front
                                + "\",\"back\":\"Verso\",\"source\":\"Fonte\"}"))
                .andExpect(status().isCreated());
    }

    void changeStatus(String adminToken, String deckId, String target) throws Exception {
        String body = beans.mockMvc()
                .perform(get("/api/admin/official-decks/" + deckId).header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode deck = beans.objectMapper().readTree(body);
        beans.mockMvc()
                .perform(put("/api/admin/official-decks/" + deckId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deck.get("version").asInt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().isOk());
    }

    record DeckSpec(String subject, String name, String description, int cards) {}
}

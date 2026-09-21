package br.com.certamecards.library.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class LibraryTestAdminCards {

    private final LibraryTestBeans beans;

    LibraryTestAdminCards(LibraryTestBeans beans) {
        this.beans = beans;
    }

    JsonNode cards(String adminToken, String deckId) throws Exception {
        String body = beans.mockMvc()
                .perform(get("/api/admin/official-decks/" + deckId + "/cards?size=50")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return beans.objectMapper().readTree(body).get("items");
    }

    String firstCardId(String adminToken, String deckId) throws Exception {
        return cards(adminToken, deckId).get(0).get("id").asString();
    }

    ResultActions edit(String adminToken, String deckId, String cardId, String body) throws Exception {
        return beans.mockMvc()
                .perform(patch("/api/admin/official-cards/" + cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", versionOf(adminToken, deckId, cardId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    ResultActions remove(String adminToken, String deckId, String cardId) throws Exception {
        return beans.mockMvc()
                .perform(delete("/api/admin/official-cards/" + cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", versionOf(adminToken, deckId, cardId)));
    }

    private int versionOf(String adminToken, String deckId, String cardId) throws Exception {
        for (JsonNode card : cards(adminToken, deckId)) {
            if (card.get("id").asString().equals(cardId)) {
                return card.get("version").asInt();
            }
        }
        throw new IllegalStateException("card not found " + cardId);
    }
}

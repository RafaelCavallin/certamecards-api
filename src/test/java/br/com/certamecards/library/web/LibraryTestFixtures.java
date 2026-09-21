package br.com.certamecards.library.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class LibraryTestFixtures {

    private final LibraryTestBeans beans;

    LibraryTestFixtures(LibraryTestBeans beans) {
        this.beans = beans;
    }

    ResultActions read(String token, String url) throws Exception {
        return beans.mockMvc().perform(get(url).header("Authorization", "Bearer " + token));
    }

    ResultActions subscribe(String token, String deckId) throws Exception {
        return beans.mockMvc()
                .perform(post("/api/library/decks/" + deckId + "/subscription")
                        .header("Authorization", "Bearer " + token));
    }

    ResultActions cancel(String token, String deckId) throws Exception {
        return beans.mockMvc()
                .perform(delete("/api/library/decks/" + deckId + "/subscription")
                        .header("Authorization", "Bearer " + token));
    }

    JsonNode json(ResultActions result) throws Exception {
        return beans.objectMapper().readTree(result.andReturn().getResponse().getContentAsString());
    }

    UUID userId(String email) {
        return beans.jdbcClient()
                .sql("SELECT id FROM users WHERE email = :email")
                .param("email", email)
                .query(UUID.class)
                .single();
    }

    int subscriberCount(String deckId) {
        return beans.jdbcClient()
                .sql("SELECT subscriber_count FROM decks WHERE id = :id")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
    }

    void fabricateOwnCards(UUID userId, int total) {
        UUID deckId = UUID.randomUUID();
        beans.jdbcClient()
                .sql("INSERT INTO decks (id, owner_id, subject_id, name) "
                        + "VALUES (:id, :userId, (SELECT id FROM subjects LIMIT 1), 'Deck enorme')")
                .param("id", deckId)
                .param("userId", userId)
                .update();
        beans.jdbcClient()
                .sql("INSERT INTO cards (id, deck_id, front, back) "
                        + "SELECT gen_random_uuid(), :deckId, 'f', 'b' FROM generate_series(1, :total)")
                .param("deckId", deckId)
                .param("total", total)
                .update();
    }
}

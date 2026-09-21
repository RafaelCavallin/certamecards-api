package br.com.certamecards.library.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class LibraryTestProgress {

    private static final Duration SCHEDULED = Duration.ofDays(10);

    private final LibraryTestBeans beans;

    LibraryTestProgress(LibraryTestBeans beans) {
        this.beans = beans;
    }

    JsonNode study(String token, String cardId, Instant reviewedAt, int reviewCount) throws Exception {
        String review = "{\"id\":\"" + UUID.randomUUID() + "\",\"cardId\":\"" + cardId
                + "\",\"kind\":\"review\",\"rating\":3,\"reviewedAt\":\"" + reviewedAt
                + "\",\"durationMs\":4000,\"stateBefore\":null,\"stateAfter\":" + state(reviewedAt)
                + ",\"offline\":false,\"deviceId\":\"" + UUID.randomUUID() + "\",\"sessionId\":null}";
        String body = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[" + review + "],\"voids\":[],"
                + "\"states\":[" + stateWithCount(cardId, reviewedAt, reviewCount) + "]}";
        return push(token, body);
    }

    JsonNode push(String token, String body) throws Exception {
        String response = beans.mockMvc()
                .perform(post("/api/sync/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return beans.objectMapper().readTree(response);
    }

    void suspend(String token, String cardId) throws Exception {
        beans.mockMvc()
                .perform(put("/api/cards/" + cardId + "/suspension")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspended\":true}"))
                .andExpect(status().isOk());
    }

    Map<String, Object> stateRow(UUID userId, String cardId) {
        return beans.jdbcClient()
                .sql("SELECT * FROM card_states WHERE user_id = :userId AND card_id = :cardId")
                .param("userId", userId)
                .param("cardId", UUID.fromString(cardId))
                .query()
                .singleRow();
    }

    int logCount(UUID userId, String cardId, String kind) {
        return beans.jdbcClient()
                .sql("SELECT count(*) FROM review_logs WHERE user_id = :userId AND card_id = :cardId AND kind = :kind")
                .param("userId", userId)
                .param("cardId", UUID.fromString(cardId))
                .param("kind", kind)
                .query(Integer.class)
                .single();
    }

    private String stateWithCount(String cardId, Instant reviewedAt, int reviewCount) {
        return "{\"cardId\":\"" + cardId + "\",\"state\":2,\"stability\":5.0,\"difficulty\":5.0,\"due\":\""
                + reviewedAt.plus(SCHEDULED) + "\",\"lastReview\":\"" + reviewedAt
                + "\",\"reps\":1,\"lapses\":0,\"learningSteps\":0,\"scheduledDays\":10,\"reviewCount\":" + reviewCount
                + "}";
    }

    private String state(Instant reviewedAt) {
        return "{\"state\":2,\"stability\":5.0,\"difficulty\":5.0,\"due\":\"" + reviewedAt.plus(SCHEDULED)
                + "\",\"lastReview\":\"" + reviewedAt + "\",\"reps\":1,\"lapses\":0,\"learningSteps\":0,"
                + "\"scheduledDays\":10}";
    }
}

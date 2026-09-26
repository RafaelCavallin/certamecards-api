package br.com.certamecards.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.subject.persistence.SubjectRepository;
import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, ResetAndVoidIT.MutableClockConfig.class})
class ResetAndVoidIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final String STATE_JSON =
            "{\"state\":2,\"stability\":4.2,\"difficulty\":5.1,\"due\":\"2026-09-21T12:10:00Z\","
                    + "\"lastReview\":\"2026-09-17T12:10:00Z\",\"reps\":3,\"lapses\":0,"
                    + "\"learningSteps\":0,\"scheduledDays\":4}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MutableClock mutableClock;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenReviewResetAndReviewIntercalated_whenFetchingHistory_thenOrderedByCanonicalClock() throws Exception {
        String token = registerConfirmAndLogin("marco.candidato@exemplo.com", "Marco");
        UUID deckId = createDeck(token);
        UUID cardId = createCard(token, deckId);
        UUID firstReviewId = UUID.randomUUID();
        UUID secondReviewId = UUID.randomUUID();
        Instant beforeReset = Instant.parse("2026-09-10T10:00:00Z");
        Instant afterReset = Instant.parse("2026-09-12T10:00:00Z");
        Instant resetAt = Instant.parse("2026-09-11T10:00:00Z");

        pushReviewWithState(token, firstReviewId, cardId, beforeReset);
        postDeckReset(token, deckId, resetAt);
        pushSingleReview(token, secondReviewId, cardId, afterReset);

        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewLogs").size()).isEqualTo(3);
        assertThat(history.get("reviewLogs").get(0).get("id").asString()).isEqualTo(firstReviewId.toString());
        assertThat(history.get("reviewLogs").get(1).get("kind").asString()).isEqualTo("reset");
        assertThat(history.get("reviewLogs").get(1).get("rating").isNull()).isTrue();
        assertThat(history.get("reviewLogs").get(2).get("id").asString()).isEqualTo(secondReviewId.toString());
    }

    @Test
    void givenVoidedReview_whenFetchingHistory_thenVoidIsRecordedWithoutRemovingTheFact() throws Exception {
        String token = registerConfirmAndLogin("nina.candidata@exemplo.com", "Nina");
        UUID deckId = createDeck(token);
        UUID cardId = createCard(token, deckId);
        UUID reviewId = UUID.randomUUID();
        pushSingleReview(token, reviewId, cardId, Instant.now().minusSeconds(60));

        String voidBody = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[],\"voids\":[{\"reviewId\":\""
                + reviewId + "\",\"voidedAt\":\"" + Instant.now() + "\"}],\"states\":[]}";
        JsonNode voidResult = pushReviews(token, voidBody);
        assertThat(voidResult.get("acceptedVoids").get(0).asString()).isEqualTo(reviewId.toString());

        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewLogs").get(0).get("id").asString()).isEqualTo(reviewId.toString());
        assertThat(history.get("reviewVoids").get(0).get("reviewId").asString()).isEqualTo(reviewId.toString());
    }

    private void postDeckReset(String token, UUID deckId, Instant clockWallTime) throws Exception {
        String operation = "{\"operationId\":\"" + UUID.randomUUID() + "\",\"kind\":\"deck_reset\",\"entityId\":\""
                + deckId + "\",\"parentId\":null,\"baseVersion\":null,\"predecessorOperationId\":null,"
                + "\"dependsOn\":[],\"occurredAt\":\"" + clockWallTime + "\","
                + "\"clock\":{\"wallTime\":\"" + clockWallTime + "\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"" + clockWallTime + "\",\"payload\":{}}";
        String batch = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"operations\":[" + operation + "]}";
        mockMvc.perform(post("/api/sync/mutations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isOk());
    }

    private void pushSingleReview(String token, UUID reviewId, UUID cardId, Instant clockWallTime) throws Exception {
        UUID deviceId = UUID.randomUUID();
        String body = "{\"deviceId\":\"" + deviceId + "\",\"reviews\":["
                + reviewJson(reviewId, cardId, deviceId, clockWallTime) + "],\"voids\":[],\"states\":[]}";
        pushReviews(token, body);
    }

    private void pushReviewWithState(String token, UUID reviewId, UUID cardId, Instant clockWallTime) throws Exception {
        UUID deviceId = UUID.randomUUID();
        String stateEntry = "{\"cardId\":\"" + cardId + "\",\"state\":2,\"stability\":4.2,\"difficulty\":5.1,"
                + "\"due\":\"2026-09-21T12:10:00Z\",\"lastReview\":\"2026-09-17T12:10:00Z\",\"reps\":3,"
                + "\"lapses\":0,\"learningSteps\":0,\"scheduledDays\":4,\"reviewCount\":1}";
        String body = "{\"deviceId\":\"" + deviceId + "\",\"reviews\":["
                + reviewJson(reviewId, cardId, deviceId, clockWallTime) + "],\"voids\":[],\"states\":[" + stateEntry
                + "]}";
        pushReviews(token, body);
    }

    private String reviewJson(UUID reviewId, UUID cardId, UUID deviceId, Instant clockWallTime) {
        return "{\"id\":\"" + reviewId + "\",\"cardId\":\"" + cardId + "\",\"kind\":\"review\",\"rating\":3"
                + ",\"reviewedAt\":\"" + clockWallTime + "\",\"durationMs\":4000,\"stateBefore\":null"
                + ",\"stateAfter\":" + STATE_JSON + ",\"offline\":true,\"deviceId\":\"" + deviceId
                + "\",\"sessionId\":null,\"clock\":{\"wallTime\":\"" + clockWallTime + "\",\"logicalCounter\":0}"
                + ",\"observedServerTime\":\"" + clockWallTime + "\"}";
    }

    private JsonNode pushReviews(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/sync/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getHistory(String token, UUID cardId) throws Exception {
        String response = mockMvc.perform(
                        get("/api/cards/" + cardId + "/reviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private UUID createDeck(String token) throws Exception {
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId + "\",\"name\":\"Deck\"}"))
                .andExpect(status().isCreated());
        return deckId;
    }

    private UUID createCard(String token, UUID deckId) throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + cardId + "\",\"front\":\"Frente\",\"back\":\"Verso\"}"))
                .andExpect(status().isCreated());
        return cardId;
    }

    private String registerConfirmAndLogin(String email, String displayName) throws Exception {
        clearInvocations(mailSender);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, displayName)));
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        Matcher matcher = TOKEN_PATTERN.matcher((String) captor.getValue().getContent());
        assertThat(matcher.find()).isTrue();
        mockMvc.perform(post("/api/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + matcher.group(1) + "\"}"));
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(body).get("accessToken").asString();
        mockMvc.perform(post("/api/me/terms")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2026-09-01\"}"));
        mutableClock.advanceBy(Duration.ofSeconds(31));
        return accessToken;
    }

    private String registerBody(String email, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\",\"displayName\":\"" + displayName
                + "\",\"acceptedTermsVersion\":\"2026-09-01\",\"timeZone\":\"America/Sao_Paulo\"}";
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\"}";
    }

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now(), ZoneOffset.UTC);
        }
    }
}

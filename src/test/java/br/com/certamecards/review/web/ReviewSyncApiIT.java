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
import org.junit.jupiter.api.DisplayName;
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
@Import({PostgresContainerSupport.class, ReviewSyncApiIT.MutableClockConfig.class})
class ReviewSyncApiIT {

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
    @DisplayName("TI-16 — envio idempotente")
    void givenSameBatch_whenPushingTwice_thenIsIdempotent() throws Exception {
        String token = registerConfirmAndLogin("ida.candidata@exemplo.com", "Ida");
        UUID cardId = createDeckAndCard(token);
        UUID reviewId = UUID.randomUUID();
        String body = reviewBody(reviewId, cardId, Instant.now().minusSeconds(30), 1, 1);

        JsonNode first = pushReviews(token, body);
        JsonNode second = pushReviews(token, body);

        assertThat(first.get("acceptedReviews").get(0).get("id").asString()).isEqualTo(reviewId.toString());
        assertThat(second.get("acceptedReviews").get(0).get("id").asString()).isEqualTo(reviewId.toString());
        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewLogs").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("TI-17 — dois dispositivos, ordens diferentes")
    void givenTwoDevicesInDifferentOrder_whenPushingStates_thenLoserBecomesStaleThenResolves() throws Exception {
        String token = registerConfirmAndLogin("teo.candidato@exemplo.com", "Teo");
        UUID cardId = createDeckAndCard(token);
        Instant now = Instant.now();
        pushReviews(token, reviewBody(UUID.randomUUID(), cardId, now.minusSeconds(120), 1, 1));
        String conflictingBody = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":["
                + reviewJson(UUID.randomUUID(), cardId, now.minusSeconds(60), 1, STATE_JSON) + "],\"voids\":[],"
                + "\"states\":[" + stateJson(cardId, 1) + "]}";

        JsonNode conflicting = pushReviews(token, conflictingBody);

        assertThat(conflicting.get("staleStates").size()).isEqualTo(1);
        assertThat(conflicting
                        .get("staleStates")
                        .get(0)
                        .get("serverReviewCount")
                        .asLong())
                .isEqualTo(2L);
        JsonNode resolved = pushReviews(token, stateOnlyBody(cardId, 2));
        assertThat(resolved.get("appliedStates").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("TI-18 — anulação depois do envio")
    void givenVoidAfterPush_whenSendingVoid_thenIsAcceptedAndPersisted() throws Exception {
        String token = registerConfirmAndLogin("vera.candidata@exemplo.com", "Vera");
        UUID cardId = createDeckAndCard(token);
        UUID reviewId = UUID.randomUUID();
        pushReviews(token, reviewBody(reviewId, cardId, Instant.now().minusSeconds(30), 1, 1));

        JsonNode voidResult = pushReviews(token, voidBody(reviewId, cardId, 0));

        assertThat(voidResult.get("acceptedVoids").get(0).asString()).isEqualTo(reviewId.toString());
        assertThat(voidResult.get("appliedStates").size()).isEqualTo(1);
        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewVoids").get(0).get("reviewId").asString()).isEqualTo(reviewId.toString());
    }

    @Test
    void givenVoidForUnownedReview_whenPushing_thenSkippedWithoutAccepting() throws Exception {
        String token = registerConfirmAndLogin("nadia.candidata@exemplo.com", "Nadia");
        UUID cardId = createDeckAndCard(token);

        JsonNode result = pushReviews(token, voidBody(UUID.randomUUID(), cardId, 0));

        assertThat(result.get("acceptedVoids").size()).isZero();
    }

    @Test
    void givenFirstReviewWithoutPriorSession_whenPushingStateWithNullLastReview_thenIsApplied() throws Exception {
        String token = registerConfirmAndLogin("pedro.candidato@exemplo.com", "Pedro");
        UUID cardId = createDeckAndCard(token);
        UUID reviewId = UUID.randomUUID();
        String body = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":["
                + reviewJson(reviewId, cardId, Instant.now().minusSeconds(30), 1) + "],\"voids\":[],\"states\":["
                + stateJson(cardId, 1, null) + "]}";

        JsonNode result = pushReviews(token, body);

        assertThat(result.get("appliedStates").size()).isEqualTo(1);
    }

    @Test
    void givenResetProgress_whenFetchingHistory_thenResetLogHasNoRating() throws Exception {
        String token = registerConfirmAndLogin("rui.candidato@exemplo.com", "Rui");
        UUID deckId = createDeck(token);
        UUID cardId = createCard(token, deckId);
        pushReviews(token, reviewBody(UUID.randomUUID(), cardId, Instant.now().minusSeconds(30), 1, 1));

        mockMvc.perform(post("/api/decks/" + deckId + "/reset-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode history = getHistory(token, cardId);
        boolean hasResetWithoutRating = false;
        for (JsonNode log : history.get("reviewLogs")) {
            if ("reset".equals(log.get("kind").asString()) && log.get("rating").isNull()) {
                hasResetWithoutRating = true;
            }
        }
        assertThat(hasResetWithoutRating).isTrue();
    }

    @Test
    void givenAnotherUsersCard_whenPushingReviewAndState_thenIgnoredWithoutRevealingExistence() throws Exception {
        String ownerToken = registerConfirmAndLogin("olga.dona@exemplo.com", "Olga");
        UUID cardId = createDeckAndCard(ownerToken);
        String otherToken = registerConfirmAndLogin("beto.outro@exemplo.com", "Beto");

        JsonNode result = pushReviews(
                otherToken,
                reviewAndStateBody(UUID.randomUUID(), cardId, Instant.now().minusSeconds(10), 1, 1));

        assertThat(result.get("rejectedReviews").get(0).get("code").asString()).isEqualTo("unknown_card");
        assertThat(result.get("ignoredStates").get(0).get("reason").asString()).isEqualTo("card_not_found");
        JsonNode history = getHistory(ownerToken, cardId);
        assertThat(history.get("reviewLogs").size()).isZero();
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

    private String reviewBody(UUID reviewId, UUID cardId, Instant reviewedAt, int reviewCount, int rating) {
        return "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":["
                + reviewJson(reviewId, cardId, reviewedAt, rating) + "],\"voids\":[],\"states\":["
                + stateJson(cardId, reviewCount) + "]}";
    }

    private String reviewAndStateBody(UUID reviewId, UUID cardId, Instant reviewedAt, int reviewCount, int rating) {
        return reviewBody(reviewId, cardId, reviewedAt, reviewCount, rating);
    }

    private String stateOnlyBody(UUID cardId, int reviewCount) {
        return "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[],\"voids\":[],\"states\":["
                + stateJson(cardId, reviewCount) + "]}";
    }

    private String voidBody(UUID reviewId, UUID cardId, int reviewCount) {
        return "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[],\"voids\":[{\"reviewId\":\"" + reviewId
                + "\",\"voidedAt\":\"" + Instant.now() + "\"}],\"states\":[" + stateJson(cardId, reviewCount) + "]}";
    }

    private String reviewJson(UUID reviewId, UUID cardId, Instant reviewedAt, int rating) {
        return reviewJson(reviewId, cardId, reviewedAt, rating, "null");
    }

    private String reviewJson(UUID reviewId, UUID cardId, Instant reviewedAt, int rating, String stateBeforeJson) {
        return "{\"id\":\"" + reviewId + "\",\"cardId\":\"" + cardId + "\",\"kind\":\"review\",\"rating\":" + rating
                + ",\"reviewedAt\":\"" + reviewedAt + "\",\"durationMs\":4000,\"stateBefore\":" + stateBeforeJson
                + ",\"stateAfter\":" + STATE_JSON + ",\"offline\":true,\"deviceId\":\"" + UUID.randomUUID()
                + "\",\"sessionId\":null,\"clock\":{\"wallTime\":\"" + reviewedAt + "\",\"logicalCounter\":0}"
                + ",\"observedServerTime\":\"" + reviewedAt + "\"}";
    }

    private String stateJson(UUID cardId, int reviewCount) {
        return stateJson(cardId, reviewCount, "2026-09-17T12:10:00Z");
    }

    private String stateJson(UUID cardId, int reviewCount, String lastReview) {
        String lastReviewJson = lastReview == null ? "null" : "\"" + lastReview + "\"";
        return "{\"cardId\":\"" + cardId + "\",\"state\":2,\"stability\":4.2,\"difficulty\":5.1,"
                + "\"due\":\"2026-09-21T12:10:00Z\",\"lastReview\":" + lastReviewJson + ",\"reps\":3,"
                + "\"lapses\":0,\"learningSteps\":0,\"scheduledDays\":4,\"reviewCount\":" + reviewCount + "}";
    }

    private UUID createDeckAndCard(String token) throws Exception {
        UUID deckId = createDeck(token);
        return createCard(token, deckId);
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

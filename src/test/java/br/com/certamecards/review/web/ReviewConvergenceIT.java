package br.com.certamecards.review.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
@Import({PostgresContainerSupport.class, ReviewConvergenceIT.MutableClockConfig.class})
class ReviewConvergenceIT {

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
    void givenTwoDevicesWithDivergentClocksPostedInReverseOrder_whenFetchingHistory_thenOrderIsCanonical()
            throws Exception {
        String token = registerConfirmAndLogin("ivo.candidato@exemplo.com", "Ivo");
        UUID cardId = createDeckAndCard(token);
        UUID deviceA = UUID.randomUUID();
        UUID deviceB = UUID.randomUUID();
        UUID earlierReviewId = UUID.randomUUID();
        UUID laterReviewId = UUID.randomUUID();
        Instant earlier = Instant.parse("2026-09-10T10:00:00Z");
        Instant later = Instant.parse("2026-09-15T10:00:00Z");

        pushSingleReview(token, laterReviewId, cardId, deviceB, later);
        pushSingleReview(token, earlierReviewId, cardId, deviceA, earlier);

        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewLogs").get(0).get("id").asString()).isEqualTo(earlierReviewId.toString());
        assertThat(history.get("reviewLogs").get(1).get("id").asString()).isEqualTo(laterReviewId.toString());
    }

    @Test
    void givenReviewOfDeletedCard_whenFetchingHistoryAfterwards_thenReviewRemainsAndStateIsNotReactivated()
            throws Exception {
        String token = registerConfirmAndLogin("juna.candidata@exemplo.com", "Juna");
        UUID cardId = createDeckAndCard(token);
        UUID reviewId = UUID.randomUUID();
        pushSingleReview(
                token, reviewId, cardId, UUID.randomUUID(), Instant.now().minusSeconds(30));

        mockMvc.perform(delete("/api/cards/" + cardId)
                        .header("Authorization", "Bearer " + token)
                        .header("If-Match", "0"))
                .andExpect(status().isNoContent());

        JsonNode reactivationAttempt = pushReviews(token, stateOnlyBody(cardId, 1));
        assertThat(reactivationAttempt.get("ignoredStates").get(0).get("reason").asString())
                .isEqualTo("card_deleted");

        JsonNode history = getHistory(token, cardId);
        assertThat(history.get("reviewLogs").get(0).get("id").asString()).isEqualTo(reviewId.toString());
    }

    private void pushSingleReview(String token, UUID reviewId, UUID cardId, UUID deviceId, Instant clockWallTime)
            throws Exception {
        String body = "{\"deviceId\":\"" + deviceId + "\",\"reviews\":["
                + reviewJson(reviewId, cardId, deviceId, clockWallTime) + "],\"voids\":[],\"states\":[]}";
        pushReviews(token, body);
    }

    private String reviewJson(UUID reviewId, UUID cardId, UUID deviceId, Instant clockWallTime) {
        return "{\"id\":\"" + reviewId + "\",\"cardId\":\"" + cardId + "\",\"kind\":\"review\",\"rating\":3"
                + ",\"reviewedAt\":\"" + clockWallTime + "\",\"durationMs\":4000,\"stateBefore\":null"
                + ",\"stateAfter\":" + STATE_JSON + ",\"offline\":true,\"deviceId\":\"" + deviceId
                + "\",\"sessionId\":null,\"clock\":{\"wallTime\":\"" + clockWallTime + "\",\"logicalCounter\":0}"
                + ",\"observedServerTime\":\"" + clockWallTime + "\"}";
    }

    private String stateOnlyBody(UUID cardId, int reviewCount) {
        return "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[],\"voids\":[],\"states\":["
                + "{\"cardId\":\"" + cardId + "\",\"state\":2,\"stability\":4.2,\"difficulty\":5.1,"
                + "\"due\":\"2026-09-21T12:10:00Z\",\"lastReview\":\"2026-09-17T12:10:00Z\",\"reps\":3,"
                + "\"lapses\":0,\"learningSteps\":0,\"scheduledDays\":4,\"reviewCount\":" + reviewCount + "}]}";
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

    private UUID createDeckAndCard(String token) throws Exception {
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId + "\",\"name\":\"Deck\"}"))
                .andExpect(status().isCreated());
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

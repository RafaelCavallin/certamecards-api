package br.com.certamecards.sync.web;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, SubscriptionSyncApiIT.MutableClockConfig.class})
class SubscriptionSyncApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MutableClock mutableClock;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenActiveThenCancelledSubscription_whenPulling_thenDeckArrivesAndLeavesWithoutLeakingOwnersReviews()
            throws Exception {
        String ownerToken = registerConfirmAndLogin("dona.oficial@exemplo.com", "Dona");
        String subscriberToken = registerConfirmAndLogin("ines.inscrita@exemplo.com", "Ines");
        UUID deckId = createDeck(ownerToken);
        UUID cardId = createCard(ownerToken, deckId);
        UUID subscriberId = userIdOf(subscriberToken);
        insertActiveSubscription(subscriberId, deckId);

        JsonNode firstPull = pull(subscriberToken, 0);

        assertThat(containsId(firstPull, "deck", deckId)).isTrue();
        assertThat(containsId(firstPull, "card", cardId)).isTrue();
        assertThat(subscriptionCancelledAt(firstPull, deckId)).isNull();

        pushOwnerReview(ownerToken, cardId);
        JsonNode afterOwnerReview = pull(subscriberToken, 0);
        assertThat(changesOfType(afterOwnerReview, "review_log")).isEmpty();

        cancelSubscription(subscriberId, deckId);
        JsonNode afterCancel = pull(subscriberToken, firstPull.get("nextCursor").asLong());

        assertThat(containsId(afterCancel, "deck", deckId)).isFalse();
        assertThat(subscriptionCancelledAt(afterCancel, deckId)).isNotNull();
    }

    private List<JsonNode> changesOfType(JsonNode page, String type) {
        List<JsonNode> payloads = new ArrayList<>();
        for (JsonNode change : page.get("changes")) {
            if (type.equals(change.get("type").asString())) {
                payloads.add(change.get("payload"));
            }
        }
        return payloads;
    }

    private boolean containsId(JsonNode page, String type, UUID id) {
        for (JsonNode payload : changesOfType(page, type)) {
            if (payload.get("id").asString().equals(id.toString())) {
                return true;
            }
        }
        return false;
    }

    private String subscriptionCancelledAt(JsonNode page, UUID deckId) {
        for (JsonNode payload : changesOfType(page, "subscription")) {
            if (payload.get("deckId").asString().equals(deckId.toString())) {
                return payload.get("cancelledAt").isNull()
                        ? null
                        : payload.get("cancelledAt").asString();
            }
        }
        return null;
    }

    private void insertActiveSubscription(UUID userId, UUID deckId) {
        new JdbcTemplate(dataSource)
                .update(
                        "INSERT INTO deck_subscriptions (user_id, deck_id, subscribed_at) VALUES (?, ?, now())",
                        userId,
                        deckId);
    }

    private void cancelSubscription(UUID userId, UUID deckId) {
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE deck_subscriptions SET cancelled_at = now() WHERE user_id = ? AND deck_id = ?",
                        userId,
                        deckId);
    }

    private JsonNode pull(String token, long cursor) throws Exception {
        String body = mockMvc.perform(
                        get("/api/sync/changes?cursor=" + cursor).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private void pushOwnerReview(String ownerToken, UUID cardId) throws Exception {
        String stateJson = "{\"state\":2,\"stability\":4.2,\"difficulty\":5.1,\"due\":\"2026-09-21T12:10:00Z\","
                + "\"lastReview\":\"2026-09-17T12:10:00Z\",\"reps\":3,\"lapses\":0,"
                + "\"learningSteps\":0,\"scheduledDays\":4}";
        Instant reviewedAt = Instant.now().minusSeconds(30);
        String body = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"reviews\":[{\"id\":\"" + UUID.randomUUID()
                + "\",\"cardId\":\"" + cardId + "\",\"kind\":\"review\",\"rating\":3,\"reviewedAt\":\"" + reviewedAt
                + "\",\"durationMs\":4000,\"stateBefore\":null,\"stateAfter\":" + stateJson
                + ",\"offline\":true,\"deviceId\":\"" + UUID.randomUUID() + "\",\"sessionId\":null,"
                + "\"clock\":{\"wallTime\":\"" + reviewedAt + "\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"" + reviewedAt + "\"}],\"voids\":[],\"states\":[]}";
        mockMvc.perform(post("/api/sync/reviews")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private UUID userIdOf(String token) throws Exception {
        String body = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asString());
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

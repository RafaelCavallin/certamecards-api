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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
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
@Import({PostgresContainerSupport.class, GlobalChangesApiIT.MutableClockConfig.class})
class GlobalChangesApiIT {

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
    @DisplayName("TI-19 — pull paginado e concorrência: nenhuma linha é pulada")
    void givenOpenTransactionOnAnOlderRow_whenPulling_thenNewerCommittedRowIsWithheldUntilItCommits() throws Exception {
        String token = registerConfirmAndLogin("wagner.barreira@exemplo.com", "Wagner");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID firstDeckId = createDeck(token, subjectId, "Deck mais antigo");

        try (Connection blocking = dataSource.getConnection()) {
            blocking.setAutoCommit(false);
            restampDeck(blocking, firstDeckId);

            UUID secondDeckId = createDeck(token, subjectId, "Deck mais novo");

            JsonNode whilstOpen = pull(token);
            assertThat(containsDeck(whilstOpen, secondDeckId)).isFalse();

            blocking.commit();

            JsonNode afterCommit = pull(token);
            assertThat(containsDeck(afterCommit, firstDeckId)).isTrue();
            assertThat(containsDeck(afterCommit, secondDeckId)).isTrue();
        }
    }

    @Test
    void givenPushedReview_whenPulling_thenReviewLogPayloadCarriesCanonicalOrder() throws Exception {
        String token = registerConfirmAndLogin("nara.candidata@exemplo.com", "Nara");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = createDeck(token, subjectId, "Deck de revisão");
        UUID cardId = createCard(token, deckId);
        UUID reviewId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        pushReview(token, deviceId, reviewId, cardId);

        JsonNode page = pull(token);

        JsonNode payload = findReviewLogPayload(page, reviewId);
        assertThat(payload).isNotNull();
        assertThat(payload.get("operationId").asString()).isEqualTo(reviewId.toString());
        assertThat(payload.get("eventDeviceId").asString()).isEqualTo(deviceId.toString());
        assertThat(payload.get("eventCounter").asInt()).isZero();
        assertThat(payload.get("eventAt").asString()).isNotBlank();
    }

    private JsonNode findReviewLogPayload(JsonNode page, UUID reviewId) {
        for (var change : page.get("changes")) {
            if (!"review_log".equals(change.get("type").asString())) {
                continue;
            }
            var payload = change.get("payload");
            if (reviewId.toString().equals(payload.get("id").asString())) {
                return payload;
            }
        }
        return null;
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

    private void pushReview(String token, UUID deviceId, UUID reviewId, UUID cardId) throws Exception {
        Instant reviewedAt = Instant.now().minusSeconds(30);
        String stateJson = "{\"cardId\":\"" + cardId + "\",\"state\":2,\"stability\":4.2,\"difficulty\":5.1,"
                + "\"due\":\"2026-09-21T12:10:00Z\",\"lastReview\":\"2026-09-17T12:10:00Z\",\"reps\":3,"
                + "\"lapses\":0,\"learningSteps\":0,\"scheduledDays\":4,\"reviewCount\":1}";
        String reviewJson = "{\"id\":\"" + reviewId + "\",\"cardId\":\"" + cardId + "\",\"kind\":\"review\","
                + "\"rating\":3,\"reviewedAt\":\"" + reviewedAt + "\",\"durationMs\":4000,\"stateBefore\":null,"
                + "\"stateAfter\":" + stateJson + ",\"offline\":true,\"deviceId\":\"" + deviceId
                + "\",\"sessionId\":null,\"clock\":{\"wallTime\":\"" + reviewedAt + "\",\"logicalCounter\":0}"
                + ",\"observedServerTime\":\"" + reviewedAt + "\"}";
        String body = "{\"deviceId\":\"" + deviceId + "\",\"reviews\":[" + reviewJson + "],\"voids\":[],"
                + "\"states\":[" + stateJson + "]}";
        mockMvc.perform(post("/api/sync/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private boolean containsDeck(JsonNode page, UUID deckId) {
        for (var change : page.get("changes")) {
            if ("deck".equals(change.get("type").asString())
                    && deckId.toString().equals(change.get("payload").get("id").asString())) {
                return true;
            }
        }
        return false;
    }

    private void restampDeck(Connection connection, UUID deckId) throws Exception {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE decks SET updated_at = now() WHERE id = ?")) {
            statement.setObject(1, deckId);
            statement.execute();
        }
    }

    private JsonNode pull(String token) throws Exception {
        String body = mockMvc.perform(get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body);
    }

    private UUID createDeck(String token, UUID subjectId, String name) throws Exception {
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId + "\",\"name\":\"" + name
                                + "\"}"))
                .andExpect(status().isCreated());
        return deckId;
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

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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, ReviewHistoryPagingIT.MutableClockConfig.class})
class ReviewHistoryPagingIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final int TOTAL_LOGS = 5005;
    private static final int PAGE_LIMIT = 500;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MutableClock mutableClock;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenMoreThanFiveThousandFacts_whenPaginatingHistory_thenAllAreDeliveredWithoutDuplicates() throws Exception {
        String token = registerConfirmAndLogin("paula.paginada@exemplo.com", "Paula");
        UUID cardId = createDeckAndCard(token);
        UUID userId = userIdFor(token);
        insertManyReviewLogs(userId, cardId, TOTAL_LOGS);

        Set<UUID> collected = new HashSet<>();
        String cursor = null;
        boolean hasMore = true;
        int pages = 0;
        while (hasMore) {
            JsonNode page = fetchPage(token, cardId, cursor);
            for (JsonNode log : page.get("reviewLogs")) {
                collected.add(UUID.fromString(log.get("id").asString()));
            }
            hasMore = page.get("hasMore").asBoolean();
            cursor = hasMore ? page.get("nextCursor").asString() : null;
            pages++;
            assertThat(pages).isLessThan(20);
        }

        assertThat(collected).hasSize(TOTAL_LOGS);
        assertThat(pages).isGreaterThan(1);
    }

    private JsonNode fetchPage(String token, UUID cardId, String cursor) throws Exception {
        String url =
                "/api/cards/" + cardId + "/reviews?limit=" + PAGE_LIMIT + (cursor == null ? "" : "&cursor=" + cursor);
        String response = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void insertManyReviewLogs(UUID userId, UUID cardId, int count) {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        UUID deviceId = UUID.randomUUID();
        for (int i = 0; i < count; i++) {
            UUID id = UUID.randomUUID();
            Instant eventAt = base.plusSeconds(i);
            jdbcClient
                    .sql(
                            """
                            INSERT INTO review_logs
                                (id, user_id, card_id, kind, rating, reviewed_at, duration_ms, state_after, offline,
                                 device_id, received_at, event_at, event_counter, event_device_id, operation_id)
                            VALUES
                                (:id, :userId, :cardId, 'review', 1, :reviewedAt, 1000, '{}', false, :deviceId, now(),
                                 :eventAt, 0, :deviceId, :id)
                            """)
                    .param("id", id)
                    .param("userId", userId)
                    .param("cardId", cardId)
                    .param("reviewedAt", Timestamp.from(eventAt))
                    .param("deviceId", deviceId)
                    .param("eventAt", Timestamp.from(eventAt))
                    .update();
        }
    }

    private UUID userIdFor(String token) throws Exception {
        String response = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
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

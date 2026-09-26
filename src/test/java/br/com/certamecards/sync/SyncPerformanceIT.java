package br.com.certamecards.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, SyncPerformanceIT.MutableClockConfig.class})
class SyncPerformanceIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final int TOTAL_OPERATIONS = 500;
    private static final int MAX_BATCH_SIZE = 100;
    private static final int CONFLICT_COUNT = 100;

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
    void givenFiveHundredOperations_whenSubmittingInBatches_thenCompletesWithinThirtySeconds() throws Exception {
        String token = registerConfirmAndLogin("perla.candidata@exemplo.com", "Perla");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deviceId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        List<String> operations = buildOperations(deckId, subjectId);

        long start = System.nanoTime();
        for (int offset = 0; offset < operations.size(); offset += MAX_BATCH_SIZE) {
            List<String> chunk = operations.subList(offset, Math.min(offset + MAX_BATCH_SIZE, operations.size()));
            postMutations(token, batchOf(deviceId, chunk));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThanOrEqualTo(30_000);
    }

    @Test
    void givenHundredConflicts_whenListing_thenOpensWithinThreeHundredMilliseconds() throws Exception {
        String token = registerConfirmAndLogin("cacilda.candidata@exemplo.com", "Cacilda");
        UUID userId = subjectOf(token);
        insertConflicts(userId, CONFLICT_COUNT);

        long start = System.nanoTime();
        mockMvc.perform(get("/api/sync/conflicts?limit=100").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThanOrEqualTo(300);
    }

    private List<String> buildOperations(UUID deckId, UUID subjectId) {
        List<String> operations = new ArrayList<>();
        operations.add(operation(UUID.randomUUID(), "deck_create", deckId, null, null, deckPayload(subjectId)));
        for (int index = 0; index < TOTAL_OPERATIONS - 1; index++) {
            UUID cardId = UUID.randomUUID();
            operations.add(operation(UUID.randomUUID(), "card_create", cardId, deckId, null, cardPayload(index)));
        }
        return operations;
    }

    private void insertConflicts(UUID userId, int count) {
        java.sql.Timestamp expiresAt = java.sql.Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS));
        for (int index = 0; index < count; index++) {
            jdbcClient
                    .sql(
                            """
                            INSERT INTO sync_conflicts
                                (user_id, entity_type, entity_id, losing_operation_id, reason, expires_at)
                            VALUES (:userId, 'card', :entityId, :losingOperationId, 'concurrent_edit', :expiresAt)
                            """)
                    .param("userId", userId)
                    .param("entityId", UUID.randomUUID())
                    .param("losingOperationId", UUID.randomUUID())
                    .param("expiresAt", expiresAt)
                    .update();
        }
    }

    private UUID subjectOf(String jwt) {
        String[] parts = jwt.split("\\.");
        byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
        String subject = objectMapper.readTree(payloadBytes).get("sub").asString();
        return UUID.fromString(subject);
    }

    private String deckPayload(UUID subjectId) {
        return "{\"subjectId\":\"" + subjectId + "\",\"name\":\"Deck de desempenho\",\"description\":null}";
    }

    private String cardPayload(int index) {
        return "{\"front\":\"Pergunta " + index + "\",\"back\":\"Resposta " + index + "\",\"source\":null}";
    }

    private String batchOf(UUID deviceId, List<String> operations) {
        return "{\"deviceId\":\"" + deviceId + "\",\"operations\":[" + String.join(",", operations) + "]}";
    }

    private String operation(
            UUID operationId, String kind, UUID entityId, UUID parentId, Integer baseVersion, String payload) {
        return "{\"operationId\":\"" + operationId + "\",\"kind\":\"" + kind + "\",\"entityId\":\"" + entityId + "\","
                + "\"parentId\":" + (parentId == null ? "null" : "\"" + parentId + "\"") + ","
                + "\"baseVersion\":" + (baseVersion == null ? "null" : baseVersion) + ","
                + "\"predecessorOperationId\":null,"
                + "\"dependsOn\":[],\"occurredAt\":\"2026-09-24T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-24T12:00:00Z\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"2026-09-24T12:00:00Z\",\"payload\":" + payload + "}";
    }

    private void postMutations(String token, String batch) throws Exception {
        mockMvc.perform(post("/api/sync/mutations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isOk());
    }

    private String registerConfirmAndLogin(String email, String displayName) throws Exception {
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

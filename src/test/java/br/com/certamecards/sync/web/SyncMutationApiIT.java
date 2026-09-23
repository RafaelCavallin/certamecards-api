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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, SyncMutationApiIT.MutableClockConfig.class})
class SyncMutationApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

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
    void givenBatchResentWithSameOperationIds_whenApplyingTwice_thenDoesNotDuplicateDeckOrCard() throws Exception {
        String token = registerConfirmAndLogin("ines.candidata@exemplo.com", "Inês");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deviceId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID deckOperationId = UUID.randomUUID();
        UUID cardOperationId = UUID.randomUUID();
        String batch = batchOf(
                deviceId,
                deckOperation(deckOperationId, "deck_create", deckId, null, null, null, deckPayload(subjectId)),
                cardOperation(cardOperationId, "card_create", cardId, deckId, null, null, cardPayload()));

        String firstResponse = postMutations(token, batch);
        String secondResponse = postMutations(token, batch);

        var firstResults = objectMapper.readTree(firstResponse).get("results");
        var secondResults = objectMapper.readTree(secondResponse).get("results");
        assertThat(outcomeFor(firstResults, deckOperationId)).isEqualTo("applied");
        assertThat(outcomeFor(secondResults, deckOperationId)).isEqualTo("duplicate");
        assertThat(outcomeFor(secondResults, cardOperationId)).isEqualTo("duplicate");

        String decksBody = mockMvc.perform(get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long deckOccurrences = 0;
        for (var change : objectMapper.readTree(decksBody).get("changes")) {
            if ("deck".equals(change.get("type").asString())
                    && change.get("payload").get("id").asString().equals(deckId.toString())) {
                deckOccurrences++;
            }
        }
        assertThat(deckOccurrences).isEqualTo(1);
    }

    @Test
    void givenSettingsPatchWithNewerThenOlderClock_whenApplying_thenOnlyNewerWins() throws Exception {
        String token = registerConfirmAndLogin("hugo.candidato@exemplo.com", "Hugo");
        UUID deviceId = UUID.randomUUID();
        String newerBatch = batchOf(deviceId, settingsOperation(UUID.randomUUID(), 25, 9));
        String olderBatch = batchOf(deviceId, settingsOperation(UUID.randomUUID(), 40, 1));

        postMutations(token, newerBatch);
        postMutations(token, olderBatch);

        String changesBody = mockMvc.perform(
                        get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer focusMinutes = null;
        for (var change : objectMapper.readTree(changesBody).get("changes")) {
            if ("settings".equals(change.get("type").asString())) {
                focusMinutes = change.get("payload").get("focusMinutes").asInt();
            }
        }
        assertThat(focusMinutes).isEqualTo(25);
    }

    private String settingsOperation(UUID operationId, int focusMinutes, int logicalCounter) {
        return "{\"operationId\":\"" + operationId + "\",\"kind\":\"settings_patch\",\"entityId\":\""
                + UUID.randomUUID()
                + "\",\"parentId\":null,\"baseVersion\":null,\"predecessorOperationId\":null,\"dependsOn\":[],"
                + "\"occurredAt\":\"2026-09-22T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-22T12:00:00Z\",\"logicalCounter\":" + logicalCounter + "},"
                + "\"observedServerTime\":\"2026-09-22T12:00:00Z\","
                + "\"payload\":{\"newPerDay\":null,\"reviewsPerDay\":null,\"focusMinutes\":" + focusMinutes
                + ",\"examDate\":null,\"timeZone\":null,\"theme\":null}}";
    }

    private String outcomeFor(tools.jackson.databind.JsonNode results, UUID operationId) {
        for (var result : results) {
            if (result.get("operationId").asString().equals(operationId.toString())) {
                return result.get("outcome").asString();
            }
        }
        throw new IllegalStateException("Missing result for " + operationId);
    }

    private String postMutations(String token, String batch) throws Exception {
        return mockMvc.perform(post("/api/sync/mutations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String deckPayload(UUID subjectId) {
        return "{\"subjectId\":\"" + subjectId + "\",\"name\":\"Deck idempotente\",\"description\":null}";
    }

    private String cardPayload() {
        return "{\"front\":\"Pergunta\",\"back\":\"Resposta\",\"source\":null}";
    }

    private String batchOf(UUID deviceId, String... operations) {
        return "{\"deviceId\":\"" + deviceId + "\",\"operations\":[" + String.join(",", operations) + "]}";
    }

    private String deckOperation(
            UUID operationId,
            String kind,
            UUID entityId,
            UUID parentId,
            Integer baseVersion,
            UUID predecessorId,
            String payload) {
        return operation(operationId, kind, entityId, parentId, baseVersion, predecessorId, payload);
    }

    private String cardOperation(
            UUID operationId,
            String kind,
            UUID entityId,
            UUID parentId,
            Integer baseVersion,
            UUID predecessorId,
            String payload) {
        return operation(operationId, kind, entityId, parentId, baseVersion, predecessorId, payload);
    }

    private String operation(
            UUID operationId,
            String kind,
            UUID entityId,
            UUID parentId,
            Integer baseVersion,
            UUID predecessorId,
            String payload) {
        return "{\"operationId\":\"" + operationId + "\",\"kind\":\"" + kind + "\",\"entityId\":\"" + entityId + "\","
                + "\"parentId\":" + (parentId == null ? "null" : "\"" + parentId + "\"") + ","
                + "\"baseVersion\":" + (baseVersion == null ? "null" : baseVersion) + ","
                + "\"predecessorOperationId\":" + (predecessorId == null ? "null" : "\"" + predecessorId + "\"") + ","
                + "\"dependsOn\":[],\"occurredAt\":\"2026-09-22T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-22T12:00:00Z\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"2026-09-22T12:00:00Z\",\"payload\":" + payload + "}";
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

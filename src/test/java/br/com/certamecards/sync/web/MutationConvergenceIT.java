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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, MutationConvergenceIT.MutableClockConfig.class})
class MutationConvergenceIT {

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
    void givenConcurrentEditsInEitherOrder_whenApplying_thenConvergeToSameWinnerAndConflict() throws Exception {
        String winnerFirst = runConcurrentEditScenario("wanda.candidata@exemplo.com", "Wanda", true);
        String loserFirst = runConcurrentEditScenario("otavio.candidato@exemplo.com", "Otávio", false);

        assertThat(winnerFirst).isEqualTo(loserFirst);
    }

    @Test
    void givenDeckDeletedConcurrentlyWithNewCard_whenApplying_thenCardIsRejectedWithoutOrphan() throws Exception {
        String token = registerConfirmAndLogin("celia.candidata@exemplo.com", "Célia");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(
                token,
                batchOf(
                        UUID.randomUUID(),
                        operation(UUID.randomUUID(), "deck_create", deckId, null, null, null, deckPayload(subjectId))));

        UUID cardId = UUID.randomUUID();
        String batch = batchOf(
                UUID.randomUUID(),
                operation(UUID.randomUUID(), "deck_delete", deckId, null, 0, null, "{}"),
                operation(UUID.randomUUID(), "card_create", cardId, deckId, null, null, cardPayload()));
        String response = postMutations(token, batch);

        var results = objectMapper.readTree(response).get("results");
        assertThat(outcomeOf(results, 0)).isEqualTo("applied");
        String cardOutcome = outcomeOf(results, 1);
        assertThat(cardOutcome).isIn("action_required", "dependency_blocked");

        String changesBody = mockMvc.perform(
                        get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        boolean orphanFound = false;
        for (var change : objectMapper.readTree(changesBody).get("changes")) {
            if ("card".equals(change.get("type").asString())
                    && change.get("payload").get("id").asString().equals(cardId.toString())) {
                orphanFound = true;
            }
        }
        assertThat(orphanFound).isFalse();
    }

    private String runConcurrentEditScenario(String email, String displayName, boolean winnerFirst) throws Exception {
        String token = registerConfirmAndLogin(email, displayName);
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(
                token,
                batchOf(
                        UUID.randomUUID(),
                        operation(UUID.randomUUID(), "deck_create", deckId, null, null, null, deckPayload(subjectId))));

        String winnerOperation = operationWithCounter(
                UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Vencedora"), 9);
        String loserOperation = operationWithCounter(
                UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Perdedora"), 1);
        String firstBatch = batchOf(UUID.randomUUID(), winnerFirst ? winnerOperation : loserOperation);
        String secondBatch = batchOf(UUID.randomUUID(), winnerFirst ? loserOperation : winnerOperation);
        postMutations(token, firstBatch);
        postMutations(token, secondBatch);

        String changesBody = mockMvc.perform(
                        get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        for (var change : objectMapper.readTree(changesBody).get("changes")) {
            if ("deck".equals(change.get("type").asString())
                    && change.get("payload").get("id").asString().equals(deckId.toString())) {
                return change.get("payload").get("name").asString();
            }
        }
        throw new IllegalStateException("Deck not found in change feed");
    }

    private String outcomeOf(JsonNode results, int index) {
        return results.get(index).get("outcome").asString();
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
        return "{\"subjectId\":\"" + subjectId + "\",\"name\":\"Deck original\",\"description\":null}";
    }

    private String renamePayload(UUID subjectId, String name) {
        return "{\"subjectId\":\"" + subjectId + "\",\"name\":\"" + name + "\",\"description\":null}";
    }

    private String cardPayload() {
        return "{\"front\":\"Pergunta\",\"back\":\"Resposta\",\"source\":null}";
    }

    private String batchOf(UUID deviceId, String... operations) {
        return "{\"deviceId\":\"" + deviceId + "\",\"operations\":[" + String.join(",", operations) + "]}";
    }

    private String operation(
            UUID operationId,
            String kind,
            UUID entityId,
            UUID parentId,
            Integer baseVersion,
            UUID predecessorId,
            String payload) {
        return operationWithCounter(operationId, kind, entityId, parentId, baseVersion, predecessorId, payload, 0);
    }

    private String operationWithCounter(
            UUID operationId, String kind, UUID entityId, Integer baseVersion, String payload, int logicalCounter) {
        return operationWithCounter(operationId, kind, entityId, null, baseVersion, null, payload, logicalCounter);
    }

    private String operationWithCounter(
            UUID operationId,
            String kind,
            UUID entityId,
            UUID parentId,
            Integer baseVersion,
            UUID predecessorId,
            String payload,
            int logicalCounter) {
        return "{\"operationId\":\"" + operationId + "\",\"kind\":\"" + kind + "\",\"entityId\":\"" + entityId + "\","
                + "\"parentId\":" + (parentId == null ? "null" : "\"" + parentId + "\"") + ","
                + "\"baseVersion\":" + (baseVersion == null ? "null" : baseVersion) + ","
                + "\"predecessorOperationId\":" + (predecessorId == null ? "null" : "\"" + predecessorId + "\"") + ","
                + "\"dependsOn\":[],\"occurredAt\":\"2026-09-22T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-22T12:00:00Z\",\"logicalCounter\":" + logicalCounter + "},"
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

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
@Import({PostgresContainerSupport.class, SyncConflictApiIT.MutableClockConfig.class})
class SyncConflictApiIT {

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
    void givenConcurrentEdits_whenListingAndReadingConflict_thenOnlyOwnerSeesIt() throws Exception {
        String owner = registerConfirmAndLogin("clara.dona@exemplo.com", "Clara");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_create", deckId, 0, deckPayload(subjectId))));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Vencedora"), 9)));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Perdedora"), 1)));

        UUID conflictId = firstConflictId(owner);

        JsonNode detail = getConflict(owner, conflictId);
        assertThat(detail.get("entityId").asString()).isEqualTo(deckId.toString());
        assertThat(detail.get("losingSnapshot").get("name").asString()).isEqualTo("Perdedora");
        assertThat(detail.get("winningSnapshot").get("name").asString()).isEqualTo("Vencedora");

        String otherUser = registerConfirmAndLogin("davi.outro@exemplo.com", "Davi");
        mockMvc.perform(get("/api/sync/conflicts/" + conflictId).header("Authorization", "Bearer " + otherUser))
                .andExpect(status().isNotFound());
        JsonNode otherList = listConflicts(otherUser);
        assertThat(otherList.get("conflicts").size()).isZero();
    }

    @Test
    void givenConflict_whenRestoringViaMutation_thenAppliesAsNewVersion() throws Exception {
        String owner = registerConfirmAndLogin("filipe.dono@exemplo.com", "Filipe");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_create", deckId, 0, deckPayload(subjectId))));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Vencedora"), 9)));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Perdedora"), 1)));
        UUID conflictId = firstConflictId(owner);

        String restorePayload = "{\"conflictId\":\"" + conflictId + "\",\"snapshot\":"
                + renamePayload(subjectId, "Restaurada") + ",\"targetDeckId\":null}";
        String response = postMutations(
                owner,
                batchOf(operationWithCounter(UUID.randomUUID(), "conflict_restore", deckId, null, restorePayload, 20)));
        assertThat(objectMapper
                        .readTree(response)
                        .get("results")
                        .get(0)
                        .get("outcome")
                        .asString())
                .isEqualTo("applied");

        JsonNode changes = getChanges(owner);
        boolean restoredNameFound = false;
        for (var change : changes.get("changes")) {
            if ("deck".equals(change.get("type").asString())
                    && change.get("payload").get("id").asString().equals(deckId.toString())
                    && "Restaurada".equals(change.get("payload").get("name").asString())) {
                restoredNameFound = true;
            }
        }
        assertThat(restoredNameFound).isTrue();
        JsonNode conflictsAfterRestore = listConflicts(owner).get("conflicts");
        assertThat(conflictsAfterRestore.size()).isEqualTo(1);
        assertThat(conflictsAfterRestore.get(0).get("id").asString()).isEqualTo(conflictId.toString());
    }

    @Test
    void givenConflictPastRetentionWindow_whenReadingDetail_thenReturnsGoneWithConflictExpired() throws Exception {
        String owner = registerConfirmAndLogin("greta.dona@exemplo.com", "Greta");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_create", deckId, 0, deckPayload(subjectId))));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Vencedora"), 9)));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Perdedora"), 1)));
        UUID conflictId = firstConflictId(owner);

        mutableClock.advanceBy(Duration.ofDays(31));

        try {
            mockMvc.perform(get("/api/sync/conflicts/" + conflictId).header("Authorization", "Bearer " + owner))
                    .andExpect(status().isGone())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                            .value("conflict_expired"));
        } finally {
            mutableClock.advanceBy(Duration.ofDays(-31));
        }
    }

    @Test
    void givenExplicitLimit_whenListing_thenRespectsRequestedPageSize() throws Exception {
        String owner = registerConfirmAndLogin("hugo.dono@exemplo.com", "Hugo");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_create", deckId, 0, deckPayload(subjectId))));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Vencedora"), 9)));
        postMutations(
                owner,
                batchOf(operationWithCounter(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Perdedora"), 1)));

        String response = mockMvc.perform(get("/api/sync/conflicts?limit=1").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(response).get("conflicts").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("TU-58/CA-18 — edição que chega depois da exclusão fica restaurável e a exclusão repetida é inócua")
    void givenEditArrivingAfterDelete_whenSyncing_thenDeleteWinsAndEditIsPreservedAsConflict() throws Exception {
        String owner = registerConfirmAndLogin("renata.dona@exemplo.com", "Renata");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_create", deckId, 0, deckPayload(subjectId))));
        postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_delete", deckId, 0, "{}")));

        String editResponse = postMutations(
                owner,
                batchOf(operation(
                        UUID.randomUUID(), "deck_update", deckId, 0, renamePayload(subjectId, "Editada offline"))));
        String secondDeleteResponse =
                postMutations(owner, batchOf(operation(UUID.randomUUID(), "deck_delete", deckId, 0, "{}")));

        JsonNode editResult = objectMapper.readTree(editResponse).get("results").get(0);
        assertThat(editResult.get("outcome").asString()).isEqualTo("conflict");
        JsonNode detail = getConflict(owner, firstConflictId(owner));
        assertThat(detail.get("losingSnapshot").get("name").asString()).isEqualTo("Editada offline");
        JsonNode secondDelete =
                objectMapper.readTree(secondDeleteResponse).get("results").get(0);
        assertThat(secondDelete.get("outcome").asString()).isEqualTo("applied");
        assertThat(listConflicts(owner).get("conflicts").size()).isEqualTo(1);
    }

    private UUID firstConflictId(String token) throws Exception {
        JsonNode list = listConflicts(token);
        assertThat(list.get("conflicts").size()).isGreaterThan(0);
        return UUID.fromString(list.get("conflicts").get(0).get("id").asString());
    }

    private JsonNode listConflicts(String token) throws Exception {
        String response = mockMvc.perform(get("/api/sync/conflicts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getConflict(String token, UUID conflictId) throws Exception {
        String response = mockMvc.perform(
                        get("/api/sync/conflicts/" + conflictId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getChanges(String token) throws Exception {
        String response = mockMvc.perform(get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
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

    private String batchOf(String... operations) {
        return "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"operations\":[" + String.join(",", operations) + "]}";
    }

    private String operation(UUID operationId, String kind, UUID entityId, Integer baseVersion, String payload) {
        return operationWithCounter(operationId, kind, entityId, baseVersion, payload, 0);
    }

    private String operationWithCounter(
            UUID operationId, String kind, UUID entityId, Integer baseVersion, String payload, int logicalCounter) {
        return "{\"operationId\":\"" + operationId + "\",\"kind\":\"" + kind + "\",\"entityId\":\"" + entityId + "\","
                + "\"parentId\":null,"
                + "\"baseVersion\":" + (baseVersion == null ? "null" : baseVersion) + ","
                + "\"predecessorOperationId\":null,"
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

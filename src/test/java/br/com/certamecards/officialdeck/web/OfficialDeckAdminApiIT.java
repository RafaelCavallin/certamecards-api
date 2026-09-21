package br.com.certamecards.officialdeck.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.subject.persistence.SubjectRepository;
import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import jakarta.mail.internet.MimeMessage;
import java.sql.Timestamp;
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
@Import({PostgresContainerSupport.class, OfficialDeckAdminApiIT.MutableClockConfig.class})
class OfficialDeckAdminApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

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
    void givenCandidate_whenTouchingOfficialContent_thenEveryWriteIsRejected() throws Exception {
        String adminToken = admin("iris.admin@exemplo.com", "Iris");
        String candidateToken = candidate("bruno.candidato@exemplo.com", "Bruno");
        String deckId = createDraftDeck(adminToken, "Direito Constitucional");
        String cardId = createCard(adminToken, deckId, "Frente", "Verso");

        mockMvc.perform(get("/api/admin/official-decks").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
        UUID anySubjectId = subjectRepository.findAll().iterator().next().getId();
        mockMvc.perform(post("/api/admin/official-decks")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDeckBody(UUID.randomUUID(), anySubjectId, "Outro")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/decks/" + deckId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hackeado\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/decks/" + deckId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .header("If-Match", 1))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + UUID.randomUUID() + "\",\"front\":\"F\",\"back\":\"V\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .header("If-Match", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"F\",\"back\":\"Hackeado\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cards/" + cardId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .header("If-Match", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenFourCards_whenPublishing_thenRequiresFiveActiveCards() throws Exception {
        String adminToken = admin("wagner.admin@exemplo.com", "Wagner");
        String deckId = createDraftDeck(adminToken, "Português");
        for (int i = 0; i < 4; i++) {
            createCard(adminToken, deckId, "Frente " + i, "Verso " + i);
        }

        mockMvc.perform(put("/api/admin/official-decks/" + deckId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deckVersion(deckId, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"published\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("official_deck_min_cards"));

        createCard(adminToken, deckId, "Frente 5", "Verso 5");

        mockMvc.perform(put("/api/admin/official-decks/" + deckId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deckVersion(deckId, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void givenActiveSubscriber_whenUnpublishingOrDeleting_thenBothAreRejected() throws Exception {
        String adminToken = admin("otavio.admin@exemplo.com", "Otávio");
        String deckId = publishedDeckWithFiveCards(adminToken, "Direito Penal");
        fabricateActiveSubscriber(deckId);

        mockMvc.perform(put("/api/admin/official-decks/" + deckId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deckVersion(deckId, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"draft\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("official_deck_has_subscribers"));
        mockMvc.perform(delete("/api/admin/official-decks/" + deckId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deckVersion(deckId, adminToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("official_deck_has_subscribers"));
    }

    @Test
    void givenPublishedDeck_whenPatchingCardWithoutChoosingContentChanged_thenValidationFails() throws Exception {
        String adminToken = admin("carla.admin@exemplo.com", "Carla");
        String deckId = publishedDeckWithFiveCards(adminToken, "Informática");
        String cardId = firstCardId(deckId, adminToken);
        int cardVersion = cardVersion(deckId, adminToken);

        mockMvc.perform(patch("/api/admin/official-cards/" + cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", cardVersion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"back\":\"nova redação\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(patch("/api/admin/official-cards/" + cardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", cardVersion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"back\":\"nova redação\",\"contentChanged\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("note"));

        mockMvc.perform(
                        patch("/api/admin/official-cards/" + cardId)
                                .header("Authorization", "Bearer " + adminToken)
                                .header("If-Match", cardVersion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"back\":\"nova redação\",\"contentChanged\":true,\"note\":\"Lei nova alterou o prazo.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedSubscribers").value(0))
                .andExpect(jsonPath("$.contentUpdateQueued").value(true));
    }

    @Test
    void givenAdminActions_whenQueryingAuditLog_thenActionsAreRecordedAndFilterable() throws Exception {
        String adminToken = admin("ana.admin@exemplo.com", "Ana");
        String deckId = publishedDeckWithFiveCards(adminToken, "Raciocínio Lógico");
        String cardId = firstCardId(deckId, adminToken);
        mockMvc.perform(patch("/api/admin/official-cards/" + cardId)
                .header("Authorization", "Bearer " + adminToken)
                .header("If-Match", cardVersion(deckId, adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"back\":\"nova redação\",\"contentChanged\":true,\"note\":\"Motivo da mudança.\"}"));
        mockMvc.perform(get("/api/admin/subjects").header("Authorization", "Bearer " + adminToken));
        UUID subjectId = subjectRepository.findAll().stream()
                .filter(subject -> subject.getName().equals("Direito Administrativo"))
                .findFirst()
                .orElseThrow()
                .getId();
        mockMvc.perform(patch("/api/admin/subjects/" + subjectId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"));

        String body = mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).contains("official_deck_published");
        assertThat(body).contains("official_card_content_changed");
        assertThat(body).contains("subject_deactivated");

        mockMvc.perform(get("/api/admin/audit-logs?action=official_deck_published")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("official_deck_published"));

        mockMvc.perform(get("/api/admin/audit-logs?size=1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextBefore").exists());
    }

    private void fabricateActiveSubscriber(String deckId) {
        UUID candidateId = UUID.randomUUID();
        jdbcClient
                .sql("INSERT INTO users (id, email, display_name, role, email_verified_at) "
                        + "VALUES (:id, :email, 'Inscrito', 'candidate', now())")
                .param("id", candidateId)
                .param("email", "inscrito-" + candidateId + "@exemplo.com")
                .update();
        jdbcClient
                .sql("INSERT INTO deck_subscriptions (user_id, deck_id, subscribed_at) VALUES (:userId, :deckId, :now)")
                .param("userId", candidateId)
                .param("deckId", UUID.fromString(deckId))
                .param("now", Timestamp.from(Instant.now()))
                .update();
        jdbcClient
                .sql("UPDATE decks SET subscriber_count = subscriber_count + 1 WHERE id = :deckId")
                .param("deckId", UUID.fromString(deckId))
                .update();
    }

    private String publishedDeckWithFiveCards(String adminToken, String subjectName) throws Exception {
        String deckId = createDraftDeck(adminToken, subjectName);
        for (int i = 0; i < 5; i++) {
            createCard(adminToken, deckId, "Frente " + i, "Verso " + i);
        }
        mockMvc.perform(put("/api/admin/official-decks/" + deckId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("If-Match", deckVersion(deckId, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"published\"}"))
                .andExpect(status().isOk());
        return deckId;
    }

    private int deckVersion(String deckId, String adminToken) throws Exception {
        String body = mockMvc.perform(
                        get("/api/admin/official-decks/" + deckId).header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("version").asInt();
    }

    private String firstCardId(String deckId, String adminToken) throws Exception {
        return firstCardNode(deckId, adminToken).get("id").asString();
    }

    private int cardVersion(String deckId, String adminToken) throws Exception {
        return firstCardNode(deckId, adminToken).get("version").asInt();
    }

    private JsonNode firstCardNode(String deckId, String adminToken) throws Exception {
        String body = mockMvc.perform(get("/api/admin/official-decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("items").get(0);
    }

    private String createDraftDeck(String adminToken, String subjectName) throws Exception {
        UUID subjectId = subjectRepository.findAll().stream()
                .filter(subject -> subject.getName().equals(subjectName))
                .findFirst()
                .orElseThrow()
                .getId();
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/admin/official-decks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDeckBody(deckId, subjectId, subjectName)))
                .andExpect(status().isCreated());
        return deckId.toString();
    }

    private String createDeckBody(UUID id, UUID subjectId, String name) {
        return "{\"id\":\"" + id + "\",\"subjectId\":\"" + subjectId + "\",\"name\":\"" + name + "\"}";
    }

    private String createCard(String adminToken, String deckId, String front, String back) throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/admin/official-decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + cardId + "\",\"type\":\"basic\",\"front\":\"" + front + "\",\"back\":\""
                                + back + "\"}"))
                .andExpect(status().isCreated());
        return cardId.toString();
    }

    private String admin(String email, String displayName) throws Exception {
        String accessToken = registerConfirmAndLogin(email, displayName, false);
        jdbcClient
                .sql("UPDATE users SET role = 'admin' WHERE email = :email")
                .param("email", email)
                .update();
        acceptTerms(accessToken);
        return accessToken;
    }

    private String candidate(String email, String displayName) throws Exception {
        return registerConfirmAndLogin(email, displayName, true);
    }

    private String registerConfirmAndLogin(String email, String displayName, boolean acceptTermsAfterLogin)
            throws Exception {
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
        if (acceptTermsAfterLogin) {
            acceptTerms(accessToken);
        }
        return accessToken;
    }

    private void acceptTerms(String accessToken) throws Exception {
        mockMvc.perform(post("/api/me/terms")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2026-09-01\"}"));
        mutableClock.advanceBy(Duration.ofSeconds(31));
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

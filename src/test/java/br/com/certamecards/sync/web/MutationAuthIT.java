package br.com.certamecards.sync.web;

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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, MutationAuthIT.MutableClockConfig.class})
class MutationAuthIT {

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
    void givenRevokedSession_whenPostingBatch_thenRejectsWholeBatchBeforeAnyWrite() throws Exception {
        String token = registerConfirmAndLogin("otilia.candidata@exemplo.com", "Otília");
        String tamperedToken = token.substring(0, token.length() - 4) + "0000";
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        String batch = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"operations\":[{"
                + "\"operationId\":\"" + UUID.randomUUID() + "\",\"kind\":\"deck_create\",\"entityId\":\"" + deckId
                + "\",\"parentId\":null,\"baseVersion\":null,\"predecessorOperationId\":null,\"dependsOn\":[],"
                + "\"occurredAt\":\"2026-09-22T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-22T12:00:00Z\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"2026-09-22T12:00:00Z\","
                + "\"payload\":{\"subjectId\":\"" + subjectId
                + "\",\"name\":\"Nunca aplicado\",\"description\":null}}]}";

        mockMvc.perform(post("/api/sync/mutations")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isUnauthorized());

        String changesBody = mockMvc.perform(
                        get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        boolean deckPersisted = false;
        for (var change : objectMapper.readTree(changesBody).get("changes")) {
            if ("deck".equals(change.get("type").asString())
                    && change.get("payload").get("id").asString().equals(deckId.toString())) {
                deckPersisted = true;
            }
        }
        assertThat(deckPersisted).isFalse();
    }

    @Test
    void givenSuspensionOnCancelledSubscription_whenApplying_thenReturnsNotApplicable() throws Exception {
        String ownerToken = registerConfirmAndLogin("paulo.oficial@exemplo.com", "Paulo");
        String seedBody = mockMvc.perform(post("/api/test-support/seed")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"official_deck\",\"count\":5}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID deckId =
                UUID.fromString(objectMapper.readTree(seedBody).get("deckId").asString());
        UUID cardId = UUID.fromString(
                objectMapper.readTree(seedBody).get("cardIds").get(0).asString());

        String subscriberToken = registerConfirmAndLogin("quenia.candidata@exemplo.com", "Quênia");
        mockMvc.perform(post("/api/library/decks/" + deckId + "/subscription")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/library/decks/" + deckId + "/subscription")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isNoContent());

        UUID operationId = UUID.randomUUID();
        String batch = "{\"deviceId\":\"" + UUID.randomUUID() + "\",\"operations\":[{"
                + "\"operationId\":\"" + operationId + "\",\"kind\":\"card_suspension\",\"entityId\":\"" + cardId
                + "\",\"parentId\":null,\"baseVersion\":null,\"predecessorOperationId\":null,\"dependsOn\":[],"
                + "\"occurredAt\":\"2026-09-22T12:00:00Z\","
                + "\"clock\":{\"wallTime\":\"2026-09-22T12:00:00Z\",\"logicalCounter\":0},"
                + "\"observedServerTime\":\"2026-09-22T12:00:00Z\","
                + "\"payload\":{\"suspended\":true}}]}";

        String response = mockMvc.perform(post("/api/sync/mutations")
                        .header("Authorization", "Bearer " + subscriberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var result = objectMapper.readTree(response).get("results").get(0);
        assertThat(result.get("outcome").asString()).isEqualTo("action_required");
        assertThat(result.get("error").get("code").asString()).isEqualTo("not_applicable");
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

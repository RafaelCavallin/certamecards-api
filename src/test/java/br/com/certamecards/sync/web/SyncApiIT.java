package br.com.certamecards.sync.web;

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
import br.com.certamecards.sync.service.SyncPurgeService;
import br.com.certamecards.user.persistence.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, SyncApiIT.MutableClockConfig.class})
class SyncApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private SyncPurgeService syncPurgeService;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("TI-08 — renomear e desativar matéria reflete no pull")
    void givenSubjectRenamedAndDeactivated_whenPulling_thenChangeIsReflected() throws Exception {
        String candidateToken = registerConfirmAndLogin("vera.candidata@exemplo.com", "Vera");
        String adminToken = registerConfirmLoginAndPromote("aldo.admin@exemplo.com", "Aldo");
        UUID subjectId = subjectRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/admin/subjects/" + subjectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Matéria Renomeada\",\"active\":false}"))
                .andExpect(status().isOk());

        String body = mockMvc.perform(
                        get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var changes = objectMapper.readTree(body).get("changes");
        boolean found = false;
        for (var change : changes) {
            if (!"subject".equals(change.get("type").asString())) {
                continue;
            }
            var payload = change.get("payload");
            if (payload.get("id").asString().equals(subjectId.toString())) {
                assertThat(payload.get("name").asString()).isEqualTo("Matéria Renomeada");
                assertThat(payload.get("active").asBoolean()).isFalse();
                found = true;
            }
        }
        assertThat(found).isTrue();

        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + UUID.randomUUID() + "\",\"subjectId\":\"" + subjectId
                                + "\",\"name\":\"Deck em matéria inativa\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("subject_inactive"));
    }

    @Test
    void givenSmallPageLimit_whenPulling_thenPaginatesUntilHasMoreIsFalse() throws Exception {
        String token = registerConfirmAndLogin("ivo.candidato@exemplo.com", "Ivo");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId
                                + "\",\"name\":\"Deck paginado\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + UUID.randomUUID() + "\",\"front\":\"Frente\",\"back\":\"Verso\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/me/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPerDay\":30,\"reviewsPerDay\":200,\"focusMinutes\":25,\"examDate\":null,"
                                + "\"timeZone\":\"America/Sao_Paulo\",\"theme\":\"noite\"}"))
                .andExpect(status().isOk());

        long cursor = 0;
        boolean hasMore = true;
        int pages = 0;
        Set<String> seenTypes = new HashSet<>();
        Set<Long> seenChangeSeqs = new HashSet<>();
        while (hasMore) {
            String body = mockMvc.perform(get("/api/sync/changes?cursor=" + cursor + "&limit=1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            var node = objectMapper.readTree(body);
            for (var change : node.get("changes")) {
                seenTypes.add(change.get("type").asString());
                assertThat(seenChangeSeqs.add(change.get("changeSeq").asLong())).isTrue();
            }
            hasMore = node.get("hasMore").asBoolean();
            cursor = node.get("nextCursor").asLong();
            pages++;
            assertThat(pages).isLessThan(50);
        }
        assertThat(pages).isGreaterThan(1);
        assertThat(seenTypes).contains("deck", "card", "settings");
    }

    @Test
    void givenLimitAboveMaximum_whenPulling_thenValidationFailed() throws Exception {
        String token = registerConfirmAndLogin("wilma.candidata@exemplo.com", "Wilma");

        mockMvc.perform(get("/api/sync/changes?cursor=0&limit=5000").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    @DisplayName("TI-20 — cursor fora da janela de purga")
    @DirtiesContext
    void givenCursorBeforePurgeWatermark_whenPulling_thenResyncRequired() throws Exception {
        String token = registerConfirmAndLogin("nando.candidato@exemplo.com", "Nando");
        UUID subjectId = subjectRepository.findAll().get(0).getId();
        UUID deckId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + deckId + "\",\"subjectId\":\"" + subjectId
                                + "\",\"name\":\"Deck a purgar\"}"))
                .andExpect(status().isCreated());
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + cardId + "\",\"front\":\"Frente\",\"back\":\"Verso\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/cards/" + cardId)
                        .header("Authorization", "Bearer " + token)
                        .header("If-Match", "0"))
                .andExpect(status().isNoContent());

        mutableClock.advanceBy(Duration.ofDays(31));
        syncPurgeService.purgeDeletedRows();

        mockMvc.perform(get("/api/sync/changes?cursor=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("resync_required"));
    }

    private String registerConfirmLoginAndPromote(String email, String displayName) throws Exception {
        String accessToken = registerConfirmAndLogin(email, displayName, false);
        userRepository.findByEmail(email).ifPresent(user -> {
            user.promoteToAdmin();
            userRepository.save(user);
        });
        acceptTerms(accessToken);
        return accessToken;
    }

    private String registerConfirmAndLogin(String email, String displayName) throws Exception {
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

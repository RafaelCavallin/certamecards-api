package br.com.certamecards.testsupport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, TestSupportApiIT.MutableClockConfig.class})
class TestSupportApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenDueCardsScenario_whenSeeding_thenCreatesStudiedCardVisibleInPull() throws Exception {
        String token = registerConfirmAndLogin("selma.candidata@exemplo.com", "Selma");
        String subjectName = subjectRepository.findAll().get(0).getName();

        String body = mockMvc.perform(post("/api/test-support/seed")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"due_cards\",\"count\":3,\"subjectName\":\"" + subjectName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String deckId = objectMapper.readTree(body).get("deckId").asString();

        mockMvc.perform(post("/api/decks/" + deckId + "/reset-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetCards").value(3));

        String pullBody = mockMvc.perform(get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long reviewLogCount = 0;
        for (var change : objectMapper.readTree(pullBody).get("changes")) {
            if ("review_log".equals(change.get("type").asString())) {
                reviewLogCount++;
            }
        }
        assertThat(reviewLogCount).isEqualTo(3);
    }

    @Test
    void givenLeechCardScenario_whenSeeding_thenCardStateHasManyLapses() throws Exception {
        String token = registerConfirmAndLogin("bento.candidato@exemplo.com", "Bento");

        String body = mockMvc.perform(post("/api/test-support/seed")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"leech_card\",\"count\":1}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cardId = objectMapper.readTree(body).get("cardIds").get(0).asString();

        mockMvc.perform(get("/api/sync/changes?cursor=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(cardId).isNotBlank();
    }

    @Test
    void givenLargeDeckScenario_whenSeeding_thenDefaultCountIsApplied() throws Exception {
        String token = registerConfirmAndLogin("hilda.candidata@exemplo.com", "Hilda");

        mockMvc.perform(post("/api/test-support/seed")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"large_deck\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardIds.length()").value(10));
    }

    @Test
    void givenOfficialDeckScenario_whenSeeding_thenPublishedDeckAppearsInTheCatalogWithoutSubscribers()
            throws Exception {
        String token = registerConfirmAndLogin("olga.candidata@exemplo.com", "Olga");

        String deckId = seedOfficial(token, "official_deck", 6);

        mockMvc.perform(get("/api/library/decks?q=" + deckId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cardCount").value(6))
                .andExpect(jsonPath("$.items[0].subscribed").value(false));
        assertThat(subscriberCount(deckId)).isZero();
    }

    @Test
    void givenOfficialDeckWithSubscribersScenario_whenSeeding_thenDeckHasSubscribersAndBlocksUnpublishing()
            throws Exception {
        String token = registerConfirmAndLogin("otto.candidato@exemplo.com", "Otto");

        String deckId = seedOfficial(token, "official_deck_with_subscribers", 1);

        assertThat(subscriberCount(deckId)).isEqualTo(2);
        mockMvc.perform(get("/api/library/decks/" + deckId + "/preview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deck.cardCount").value(5));
    }

    private String seedOfficial(String token, String scenario, int count) throws Exception {
        String body = mockMvc.perform(post("/api/test-support/seed")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"" + scenario + "\",\"count\":" + count + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("deckId").asString();
    }

    private int subscriberCount(String deckId) {
        return jdbcClient
                .sql("SELECT subscriber_count FROM decks WHERE id = :id")
                .param("id", UUID.fromString(deckId))
                .query(Integer.class)
                .single();
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

package br.com.certamecards.errorreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.user.persistence.AccountPurgeQuery;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, ErrorReportApiIT.MutableClockConfig.class})
class ErrorReportApiIT {

    private static final String SEEDED_DECK_NAME = "CF/88 — princípios e direitos fundamentais";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private AccountPurgeQuery accountPurgeQuery;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    private ErrorReportTestSupport support;

    @BeforeEach
    void createSupport() {
        support = new ErrorReportTestSupport(mockMvc, objectMapper, jdbcClient, mailSender, mutableClock);
    }

    @Test
    void givenPublishedOfficialCard_whenReportedTwice_thenSecondIsConflictAndAdminClosesIt() throws Exception {
        String adminToken = support.admin(email("admin"), "Iris");
        String candidateToken = support.candidate(email("cand"), "Ana");
        String other = support.candidate(email("other"), "Caio");
        UUID deckId = publishSeededDeck();
        UUID cardId = firstCardOf(deckId);

        String body = "{\"reason\":\"outdated_content\",\"note\":\"EC nova mudou o texto\"}";
        report(candidateToken, cardId, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("open"));
        report(candidateToken, cardId, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("report_already_sent"));
        mutableClock.advanceBy(Duration.ofSeconds(5));
        report(other, cardId, "{\"reason\":\"typo\"}").andExpect(status().isCreated());

        String listed = read(adminToken, "/api/admin/error-reports")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].reporterName").value("Ana"))
                .andExpect(jsonPath("$.items[0].deckName").value(SEEDED_DECK_NAME))
                .andExpect(jsonPath("$.items[0].subjectName").value("Direito Constitucional"))
                .andExpect(jsonPath("$.items[0].reason").value("outdated_content"))
                .andExpect(jsonPath("$.items[0].note").value("EC nova mudou o texto"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(listed).doesNotContain("@exemplo.com");
        read(adminToken, "/api/admin/official-decks/" + deckId)
                .andExpect(jsonPath("$.openReportCount").value(2));
        String reportId =
                objectMapper.readTree(listed).get("items").get(0).get("id").asString();
        close(adminToken, reportId, "resolved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
        close(adminToken, reportId, "rejected").andExpect(status().isConflict());
        read(adminToken, "/api/admin/error-reports")
                .andExpect(jsonPath("$.total").value(1));
        read(adminToken, "/api/admin/error-reports?status=resolved")
                .andExpect(jsonPath("$.total").value(1));
        read(adminToken, "/api/admin/official-decks/" + deckId)
                .andExpect(jsonPath("$.openReportCount").value(1));
        assertThat(auditActions()).contains("error_report_resolved");
    }

    @Test
    void givenInvalidRequests_whenReportingOrListing_thenRejects() throws Exception {
        String adminToken = support.admin(email("admin"), "Iris");
        String candidateToken = support.candidate(email("cand"), "Ana");
        UUID deckId = publishSeededDeck();
        UUID cardId = firstCardOf(deckId);

        report(candidateToken, cardId, "{\"reason\":\"spam\"}").andExpect(status().isBadRequest());
        report(candidateToken, cardId, "{\"reason\":\"typo\",\"note\":\"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest());
        report(candidateToken, UUID.randomUUID(), "{\"reason\":\"typo\"}").andExpect(status().isNotFound());
        read(candidateToken, "/api/admin/error-reports").andExpect(status().isForbidden());
        read(adminToken, "/api/admin/error-reports?status=bogus").andExpect(status().isBadRequest());
        read(adminToken, "/api/admin/error-reports?size=0").andExpect(status().isBadRequest());
        close(adminToken, UUID.randomUUID().toString(), "resolved").andExpect(status().isNotFound());
        close(adminToken, UUID.randomUUID().toString(), "open").andExpect(status().isBadRequest());
    }

    @Test
    void givenDraftDeckAndNoSubscription_whenReporting_thenNotFound() throws Exception {
        String candidateToken = support.candidate(email("cand"), "Ana");
        UUID draftCard = jdbcClient
                .sql("SELECT c.id FROM cards c JOIN decks d ON d.id = c.deck_id "
                        + "WHERE d.owner_id IS NULL AND d.official_status = 'draft' LIMIT 1")
                .query(UUID.class)
                .single();

        report(candidateToken, draftCard, "{\"reason\":\"typo\"}").andExpect(status().isNotFound());
    }

    @Test
    void givenReports_whenPurgingAccounts_thenReportsAndClosureReferencesAreRemoved() throws Exception {
        String adminToken = support.admin(email("admin"), "Iris");
        String candidateEmail = email("cand");
        String candidateToken = support.candidate(candidateEmail, "Ana");
        UUID cardId = firstCardOf(publishSeededDeck());
        report(candidateToken, cardId, "{\"reason\":\"typo\"}").andExpect(status().isCreated());
        String reportId = objectMapper
                .readTree(read(adminToken, "/api/admin/error-reports")
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("items")
                .get(0)
                .get("id")
                .asString();
        close(adminToken, reportId, "resolved").andExpect(status().isOk());

        UUID candidateId = userId(candidateEmail);

        accountPurgeQuery.purge(candidateId);

        assertThat(reportsOf(candidateId)).isZero();
    }

    private UUID publishSeededDeck() {
        UUID deckId = jdbcClient
                .sql("SELECT id FROM decks WHERE name = :name")
                .param("name", SEEDED_DECK_NAME)
                .query(UUID.class)
                .single();
        jdbcClient
                .sql("UPDATE decks SET official_status = 'published' WHERE id = :id")
                .param("id", deckId)
                .update();
        return deckId;
    }

    private UUID firstCardOf(UUID deckId) {
        return jdbcClient
                .sql("SELECT id FROM cards WHERE deck_id = :deckId ORDER BY id LIMIT 1")
                .param("deckId", deckId)
                .query(UUID.class)
                .single();
    }

    private ResultActions report(String token, UUID cardId, String body) throws Exception {
        return mockMvc.perform(post("/api/cards/" + cardId + "/error-reports")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions read(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    private ResultActions close(String token, String reportId, String outcome) throws Exception {
        return mockMvc.perform(post("/api/admin/error-reports/" + reportId + "/closure")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"" + outcome + "\"}"));
    }

    private List<String> auditActions() {
        return jdbcClient
                .sql("SELECT action FROM admin_audit_logs")
                .query(String.class)
                .list();
    }

    private UUID userId(String email) {
        return jdbcClient
                .sql("SELECT id FROM users WHERE email = :email")
                .param("email", email)
                .query(UUID.class)
                .single();
    }

    private long reportsOf(UUID userId) {
        Long total = jdbcClient
                .sql("SELECT COUNT(*) FROM card_error_reports WHERE user_id = :userId")
                .param("userId", userId)
                .query(Long.class)
                .single();
        return total;
    }

    private String email(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@exemplo.com";
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

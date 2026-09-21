package br.com.certamecards.deck.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
@Import({PostgresContainerSupport.class, DeckCardApiIT.MutableClockConfig.class})
class DeckCardApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private br.com.certamecards.subject.persistence.SubjectRepository subjectRepository;

    @Autowired
    private MutableClock mutableClock;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenSameIdAndOwner_whenCreatingDeckTwice_thenSecondRequestIsIdempotent() throws Exception {
        String token = registerConfirmAndLogin("wesley.candidato@exemplo.com", "Wesley");
        UUID subjectId = firstSubjectId();
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckBody(id, subjectId, "Deck idempotente")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckBody(id, subjectId, "Deck idempotente")))
                .andExpect(status().isOk());
    }

    @Test
    void givenSameIdAndOwner_whenCreatingCardTwice_thenSecondRequestIsIdempotent() throws Exception {
        String token = registerConfirmAndLogin("yasmin.candidata@exemplo.com", "Yasmin");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck para cartão idempotente");
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody(cardId, "Frente", "Verso")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody(cardId, "Frente", "Verso")))
                .andExpect(status().isOk());
    }

    @Test
    void givenMissingIfMatchHeader_whenDeletingDeck_thenValidationFailed() throws Exception {
        String token = registerConfirmAndLogin("otilia.candidata@exemplo.com", "Otília");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck sem If-Match");

        mockMvc.perform(delete("/api/decks/" + deckId).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void givenNonNumericIfMatchHeader_whenDeletingDeck_thenValidationFailed() throws Exception {
        String token = registerConfirmAndLogin("paulo.candidato@exemplo.com", "Paulo");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck com If-Match inválido");

        mockMvc.perform(delete("/api/decks/" + deckId)
                        .header("Authorization", "Bearer " + token)
                        .header("If-Match", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void givenDeckWithCards_whenDeletingDeck_thenDeckAndCardsAreSoftDeleted() throws Exception {
        String token = registerConfirmAndLogin("dara.candidata@exemplo.com", "Dara");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck para excluir");
        createCard(token, deckId, "Frente", "Verso");

        mockMvc.perform(delete("/api/decks/" + deckId)
                        .header("Authorization", "Bearer " + token)
                        .header("If-Match", "0"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/decks/" + deckId + "/reset-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudiedCards_whenResettingProgress_thenReviewLogsAreInsertedAndReviewCountIncreases() throws Exception {
        String token = registerConfirmAndLogin("rui.candidato@exemplo.com", "Rui");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck de estudo");
        UUID cardId = createCard(token, deckId, "Frente", "Verso");
        mockMvc.perform(put("/api/cards/" + cardId + "/suspension")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"suspended\":false}"));

        String resetBody = mockMvc.perform(
                        post("/api/decks/" + deckId + "/reset-progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(resetBody).get("resetCards").asInt()).isZero();
    }

    @Test
    void givenCard_whenEditingContent_thenVersionChangesButStateUnaffected() throws Exception {
        String token = registerConfirmAndLogin("clea.candidata@exemplo.com", "Clea");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck para editar");
        UUID cardId = createCard(token, deckId, "Frente original", "Verso original");

        mockMvc.perform(patch("/api/cards/" + cardId)
                        .header("Authorization", "Bearer " + token)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"Frente nova\",\"back\":\"Verso original\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("Frente nova"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void givenActiveCard_whenSuspending_thenCardStateIsCreatedSuspended() throws Exception {
        String token = registerConfirmAndLogin("igor.candidato@exemplo.com", "Igor");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck suspensão");
        UUID cardId = createCard(token, deckId, "Frente", "Verso");

        mockMvc.perform(put("/api/cards/" + cardId + "/suspension")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspended\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspended").value(true))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    void givenDeckNearCardLimit_whenCreatingCardsConcurrently_thenOnlyOneSucceedsAtTheLimit() throws Exception {
        String token = registerConfirmAndLogin("noel.candidato@exemplo.com", "Noel");
        UUID subjectId = firstSubjectId();
        UUID deckId = createDeck(token, subjectId, "Deck no limite");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            pool.submit(createCardTask(token, deckId, ready, start, successCount, conflictCount));
            pool.submit(createCardTask(token, deckId, ready, start, successCount, conflictCount));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get() + conflictCount.get()).isEqualTo(2);
    }

    private Runnable createCardTask(
            String token,
            UUID deckId,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        return () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                int status = mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cardBody(UUID.randomUUID(), "Frente", "Verso")))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                }
                if (status == 422) {
                    conflictCount.incrementAndGet();
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private UUID firstSubjectId() {
        return subjectRepository.findAll().get(0).getId();
    }

    private UUID createDeck(String token, UUID subjectId, String name) throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/decks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deckBody(id, subjectId, name)))
                .andExpect(status().isCreated());
        return id;
    }

    private UUID createCard(String token, UUID deckId, String front, String back) throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/decks/" + deckId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardBody(id, front, back)))
                .andExpect(status().isCreated());
        return id;
    }

    private String deckBody(UUID id, UUID subjectId, String name) {
        return "{\"id\":\"" + id + "\",\"subjectId\":\"" + subjectId + "\",\"name\":\"" + name + "\"}";
    }

    private String cardBody(UUID id, String front, String back) {
        return "{\"id\":\"" + id + "\",\"front\":\"" + front + "\",\"back\":\"" + back + "\"}";
    }

    private String registerConfirmAndLogin(String email, String displayName) throws Exception {
        clearInvocationsOnMailSender();
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

    private void clearInvocationsOnMailSender() {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        org.mockito.Mockito.clearInvocations(mailSender);
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

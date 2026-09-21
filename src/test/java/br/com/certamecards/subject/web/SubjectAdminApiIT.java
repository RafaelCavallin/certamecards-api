package br.com.certamecards.subject.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.user.persistence.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
@Import({PostgresContainerSupport.class, SubjectAdminApiIT.MutableClockConfig.class})
class SubjectAdminApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MutableClock mutableClock;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenCandidate_whenAccessingAdminSubjects_thenForbidden() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        String candidateToken = registerConfirmAndLogin("iris.candidata@exemplo.com", "Iris");

        mockMvc.perform(get("/api/admin/subjects").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void givenDuplicateNormalizedName_whenCreatingSubject_thenSecondRequestIsRejected() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        String adminToken = registerConfirmLoginAndPromote("wagner.admin@exemplo.com", "Wagner");

        mockMvc.perform(post("/api/admin/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Direito Tributário\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"direito tributário\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("subject_name_taken"));
    }

    @Test
    void givenConcurrentCreationsWithSameNormalizedName_whenCreatingSubject_thenOnlyOneSucceeds() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        String adminToken = registerConfirmLoginAndPromote("otavio.admin@exemplo.com", "Otávio");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        try {
            pool.submit(
                    createSubjectTask(adminToken, "Direito Previdenciário", ready, start, successCount, conflictCount));
            pool.submit(createSubjectTask(
                    adminToken, "direito  previdenciário", ready, start, successCount, conflictCount));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    private Runnable createSubjectTask(
            String adminToken,
            String name,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        return () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                int status = mockMvc.perform(post("/api/admin/subjects")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + name + "\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                registerStatus(status, successCount, conflictCount);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private void registerStatus(int status, AtomicInteger successCount, AtomicInteger conflictCount) {
        if (status == 201) {
            successCount.incrementAndGet();
        }
        if (status == 409) {
            conflictCount.incrementAndGet();
        }
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

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now(), ZoneOffset.UTC);
        }
    }

    private String registerBody(String email, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\",\"displayName\":\"" + displayName
                + "\",\"acceptedTermsVersion\":\"2026-09-01\",\"timeZone\":\"America/Sao_Paulo\"}";
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\"}";
    }
}

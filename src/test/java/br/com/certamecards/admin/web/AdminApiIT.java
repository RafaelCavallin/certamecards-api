package br.com.certamecards.admin.web;

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

import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.user.persistence.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class AdminApiIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("TI-09 — conceder e retirar admin")
    void givenGrantedAndRevokedAdmin_whenAccessingAdminArea_thenAccessFollowsCurrentRole() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        String adminToken = registerConfirmLoginAndPromote("teodora.admin@exemplo.com", "Teodora");
        String candidateEmail = "bruno.candidato@exemplo.com";
        String candidateToken = registerConfirmAndLogin(candidateEmail, "Bruno");

        mockMvc.perform(post("/api/admin/admins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + candidateEmail + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/subjects").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        String candidateId =
                userRepository.findByEmail(candidateEmail).orElseThrow().getId().toString();
        mockMvc.perform(delete("/api/admin/admins/" + candidateId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/subjects").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
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
    }

    private String registerBody(String email, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\",\"displayName\":\"" + displayName
                + "\",\"acceptedTermsVersion\":\"2026-09-01\",\"timeZone\":\"America/Sao_Paulo\"}";
    }

    private String loginBody(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\"}";
    }
}

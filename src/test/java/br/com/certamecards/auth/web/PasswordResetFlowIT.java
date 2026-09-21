package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.CookieTestSupport;
import br.com.certamecards.support.PostgresContainerSupport;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class PasswordResetFlowIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final String EMAIL = "beatriz.candidata@exemplo.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    void givenResetTokenUsedTwice_whenResetting_thenSecondUseIsRejectedAndOldSessionsRevoked() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        registerConfirmAndLogin();
        Mockito.clearInvocations(mailSender);

        Cookie oldRefreshCookie = CookieTestSupport.fromSetCookieHeader(loginAndGetRefreshCookie());

        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isAccepted());

        String resetToken = captureResetToken();
        String resetBody = "{\"token\":\"" + resetToken + "\",\"newPassword\":\"nova-senha-456\"}";

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("token_used"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Origin", "https://certamecards.localhost")
                        .header("X-Certame-Client", "web")
                        .cookie(oldRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    private void registerConfirmAndLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody()));
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        String rawToken = extractToken((String) captor.getValue().getContent());
        mockMvc.perform(post("/api/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + rawToken + "\"}"));
    }

    private String loginAndGetRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private String captureResetToken() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        return extractToken((String) captor.getValue().getContent());
    }

    private String extractToken(String html) {
        Matcher matcher = TOKEN_PATTERN.matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String registerBody() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"senha-forte-123\",\"displayName\":\"Beatriz\","
                + "\"acceptedTermsVersion\":\"2026-09-01\",\"timeZone\":\"America/Sao_Paulo\"}";
    }

    private String loginBody() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"senha-forte-123\"}";
    }
}

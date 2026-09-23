package br.com.certamecards.errorreport.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.MutableClock;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

record ErrorReportTestSupport(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        JdbcClient jdbcClient,
        JavaMailSender mailSender,
        MutableClock clock) {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final String PASSWORD = "senha-forte-123";
    private static final String TERMS_VERSION = "2026-09-01";

    String candidate(String email, String displayName) throws Exception {
        clearInvocations(mailSender);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\""
                        + displayName + "\",\"acceptedTermsVersion\":\"" + TERMS_VERSION
                        + "\",\"timeZone\":\"America/Sao_Paulo\"}"));
        confirmEmail();
        String token = login(email);
        mockMvc.perform(post("/api/me/terms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"" + TERMS_VERSION + "\"}"));
        clock.advanceBy(Duration.ofSeconds(31));
        return token;
    }

    String admin(String email, String displayName) throws Exception {
        String token = candidate(email, displayName);
        jdbcClient
                .sql("UPDATE users SET role = 'admin' WHERE email = :email")
                .param("email", email)
                .update();
        clock.advanceBy(Duration.ofSeconds(31));
        return token;
    }

    private void confirmEmail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        Matcher matcher = TOKEN_PATTERN.matcher((String) captor.getValue().getContent());
        assertThat(matcher.find()).isTrue();
        mockMvc.perform(post("/api/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + matcher.group(1) + "\"}"));
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }
}

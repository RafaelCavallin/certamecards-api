package br.com.certamecards.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

class LibraryTestAccounts {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final String PASSWORD = "senha-forte-123";
    private static final String TERMS_VERSION = "2026-09-01";
    private static final Duration TERMS_CACHE_WINDOW = Duration.ofSeconds(31);

    private final LibraryTestBeans beans;

    LibraryTestAccounts(LibraryTestBeans beans) {
        this.beans = beans;
    }

    String candidate(String email) throws Exception {
        String token = registerConfirmAndLogin(email);
        acceptTerms(token);
        return token;
    }

    String admin(String email) throws Exception {
        String token = registerConfirmAndLogin(email);
        beans.jdbcClient()
                .sql("UPDATE users SET role = 'admin' WHERE email = :email")
                .param("email", email)
                .update();
        acceptTerms(token);
        return token;
    }

    private String registerConfirmAndLogin(String email) throws Exception {
        clearInvocations(beans.mailSender());
        doNothing().when(beans.mailSender()).send(any(MimeMessage.class));
        beans.mockMvc()
                .perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"displayName\":\"Teste\",\"acceptedTermsVersion\":\"" + TERMS_VERSION
                                + "\",\"timeZone\":\"America/Sao_Paulo\"}"));
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(beans.mailSender(), timeout(5000)).send(captor.capture());
        Matcher matcher = TOKEN_PATTERN.matcher((String) captor.getValue().getContent());
        assertThat(matcher.find()).isTrue();
        beans.mockMvc()
                .perform(post("/api/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + matcher.group(1) + "\"}"));
        String body = beans.mockMvc()
                .perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return beans.objectMapper().readTree(body).get("accessToken").asString();
    }

    private void acceptTerms(String token) throws Exception {
        beans.mockMvc()
                .perform(post("/api/me/terms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"" + TERMS_VERSION + "\"}"));
        beans.clock().advanceBy(TERMS_CACHE_WINDOW);
    }
}

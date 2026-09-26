package br.com.certamecards.auth.web;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class RefreshLogoutOriginIT {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("token=([^\"'\\s]+)");
    private static final String EMAIL = "diana.candidata@exemplo.com";
    private static final String ORIGIN = "https://certamecards.localhost";

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("TI-04 — refresh e logout exigem Origin e X-Certame-Client")
    void givenMissingOriginHeaders_whenRefreshingOrLoggingOut_thenRejectedWithOriginRejected() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));
        Cookie refreshCookie = CookieTestSupport.fromSetCookieHeader(registerConfirmLoginAndGetCookie());

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("origin_rejected"));

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("origin_rejected"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Origin", ORIGIN)
                        .header("X-Certame-Client", "web")
                        .cookie(refreshCookie))
                .andExpect(status().isOk());
    }

    private String registerConfirmLoginAndGetCookie() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody()));
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        Matcher matcher = TOKEN_PATTERN.matcher((String) captor.getValue().getContent());
        matcher.find();
        mockMvc.perform(post("/api/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + matcher.group(1) + "\"}"));
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andReturn();
        return login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private String registerBody() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"senha-forte-123\",\"displayName\":\"Diana\","
                + "\"acceptedTermsVersion\":\"2026-09-01\",\"timeZone\":\"America/Sao_Paulo\"}";
    }

    private String loginBody() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"senha-forte-123\"}";
    }
}

package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, GoogleLoginFlowIT.MutableClockConfig.class})
class GoogleLoginFlowIT {

    private static final String EMAIL = "eduarda.google@exemplo.com";
    private static final String SUBJECT = "google-subject-eduarda";
    private static final String TERMS_VERSION = "2026-09-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoogleAuthenticationSuccessHandler successHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @Autowired
    private MutableClock mutableClock;

    @Test
    @DisplayName("TI-03 — callback Google para e-mail novo e aceite dos termos")
    void givenNewGoogleEmail_whenCallbackSucceeds_thenUserCreatedAndTermsRequiredUntilAccepted() throws Exception {
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), fakeGoogleAuthentication());

        User created = userRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(created.getTerms().getAcceptedAt()).isNull();

        String accessToken = accessTokenIssuer.issue(created.getId()).token();

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("terms_required"));

        mockMvc.perform(post("/api/me/terms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"" + TERMS_VERSION + "\"}"))
                .andExpect(status().isNoContent());
        mutableClock.advanceBy(Duration.ofSeconds(31));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsAccepted").value(true));
    }

    private Authentication fakeGoogleAuthentication() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn(EMAIL);
        when(oidcUser.getSubject()).thenReturn(SUBJECT);
        when(oidcUser.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(oidcUser.getFullName()).thenReturn("Eduarda Google");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        return authentication;
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

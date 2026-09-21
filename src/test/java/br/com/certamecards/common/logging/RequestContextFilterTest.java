package br.com.certamecards.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.security.AuthenticatedJwtToken;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.user.domain.UserRole;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestContextFilterTest {

    private final RequestContextFilter filter = new RequestContextFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAuthenticatedUser_whenFiltering_thenMdcHasRequestIdTraceIdAndUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext()
                .setAuthentication(new AuthenticatedJwtToken(
                        buildJwt(), new AuthenticatedUser(userId, UserRole.CANDIDATE, true), List.of()));
        FilterChain chain = (request, response) -> {
            assertThat(MDC.get("requestId")).isNotBlank();
            assertThat(MDC.get("traceId")).isEqualTo(MDC.get("requestId"));
            assertThat(MDC.get("userId")).isEqualTo(userId.toString());
        };

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void givenNoAuthentication_whenFiltering_thenMdcHasNoUserId() throws Exception {
        FilterChain chain = (request, response) -> assertThat(MDC.get("userId")).isNull();

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
    }

    private org.springframework.security.oauth2.jwt.Jwt buildJwt() {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();
    }
}

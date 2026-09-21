package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class RegisterRateLimitFilterTest {

    private final IpRateLimiter rateLimiter = mock(IpRateLimiter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RegisterRateLimitProperties properties = new RegisterRateLimitProperties(10, Duration.ofMinutes(1));
    private final RegisterRateLimitFilter filter = new RegisterRateLimitFilter(rateLimiter, properties, objectMapper);

    @Test
    void givenNonRegisterPath_whenFiltering_thenAlwaysContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void givenRegisterPathWithinLimit_whenFiltering_thenContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void givenRegisterPathOverLimit_whenFiltering_thenRejectsWithRateLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("rate_limited");
    }
}

package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class EventsRateLimitFilterTest {

    private final IpRateLimiter rateLimiter = mock(IpRateLimiter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventsRateLimitFilter filter = new EventsRateLimitFilter(rateLimiter, objectMapper);

    @Test
    void givenNonEventsPath_whenFiltering_thenAlwaysContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/decks");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void givenEventsPathWithinLimit_whenFiltering_thenContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void givenEventsPathOverLimit_whenFiltering_thenRejectsWithRateLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("rate_limited");
    }
}

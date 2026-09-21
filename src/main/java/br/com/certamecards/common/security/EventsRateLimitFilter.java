package br.com.certamecards.common.security;

import br.com.certamecards.common.error.ApiError;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.events.domain.ProductEventLimits;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class EventsRateLimitFilter extends OncePerRequestFilter {

    private static final String PATH = "/api/events";
    private static final String RATE_LIMIT_KEY_PREFIX = "events:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public EventsRateLimitFilter(IpRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean shouldLimit = PATH.equals(request.getRequestURI());
        String key = RATE_LIMIT_KEY_PREFIX + request.getRemoteAddr();
        boolean withinLimit = rateLimiter.tryConsume(key, ProductEventLimits.MAX_REQUESTS_PER_MINUTE, WINDOW);
        if (!shouldLimit || withinLimit) {
            chain.doFilter(request, response);
            return;
        }
        writeRejection(response);
    }

    private void writeRejection(HttpServletResponse response) throws IOException {
        ApiError body = ApiError.of(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.detail(), null, null);
        response.setStatus(ErrorCode.RATE_LIMITED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

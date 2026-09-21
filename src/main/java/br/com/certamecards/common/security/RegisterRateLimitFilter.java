package br.com.certamecards.common.security;

import br.com.certamecards.common.error.ApiError;
import br.com.certamecards.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class RegisterRateLimitFilter extends OncePerRequestFilter {

    private static final String PATH = "/api/auth/register";
    private static final String RATE_LIMIT_KEY_PREFIX = "register:";

    private final IpRateLimiter rateLimiter;
    private final RegisterRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RegisterRateLimitFilter(
            IpRateLimiter rateLimiter, RegisterRateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean shouldLimit = PATH.equals(request.getRequestURI());
        String key = RATE_LIMIT_KEY_PREFIX + request.getRemoteAddr();
        if (!shouldLimit || rateLimiter.tryConsume(key, properties.maxRequests(), properties.window())) {
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

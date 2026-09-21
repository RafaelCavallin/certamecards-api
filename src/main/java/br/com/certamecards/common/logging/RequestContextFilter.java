package br.com.certamecards.common.logging;

import br.com.certamecards.common.security.AuthenticatedJwtToken;
import br.com.certamecards.common.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestContextFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(TRACE_ID_KEY, requestId);
        putUserIdIfAuthenticated();
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void putUserIdIfAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AuthenticatedJwtToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            MDC.put(USER_ID_KEY, user.id().toString());
        }
    }
}

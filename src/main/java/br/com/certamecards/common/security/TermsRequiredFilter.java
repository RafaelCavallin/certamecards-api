package br.com.certamecards.common.security;

import br.com.certamecards.common.error.ApiError;
import br.com.certamecards.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class TermsRequiredFilter extends OncePerRequestFilter {

    private static final String EXEMPT_PATH = "/api/me/terms";

    private final ObjectMapper objectMapper;

    public TermsRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (EXEMPT_PATH.equals(request.getRequestURI()) || hasAcceptedTerms()) {
            chain.doFilter(request, response);
            return;
        }
        writeRejection(response);
    }

    private boolean hasAcceptedTerms() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AuthenticatedJwtToken token)) {
            return true;
        }
        return ((AuthenticatedUser) token.getPrincipal()).termsAccepted();
    }

    private void writeRejection(HttpServletResponse response) throws IOException {
        ApiError body = ApiError.of(ErrorCode.TERMS_REQUIRED, ErrorCode.TERMS_REQUIRED.detail(), null, null);
        response.setStatus(ErrorCode.TERMS_REQUIRED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

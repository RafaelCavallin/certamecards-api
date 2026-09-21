package br.com.certamecards.common.security;

import br.com.certamecards.common.error.ApiError;
import br.com.certamecards.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class OriginValidationFilter extends OncePerRequestFilter {

    private static final String CLIENT_HEADER_NAME = "X-Certame-Client";
    private static final Set<String> PROTECTED_PATHS = Set.of("/api/auth/refresh", "/api/auth/logout");

    private final ClientOriginProperties properties;
    private final ObjectMapper objectMapper;

    public OriginValidationFilter(ClientOriginProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!PROTECTED_PATHS.contains(request.getRequestURI()) || isValidOrigin(request)) {
            chain.doFilter(request, response);
            return;
        }
        writeRejection(response);
    }

    private boolean isValidOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String client = request.getHeader(CLIENT_HEADER_NAME);
        return properties.frontendOrigin().equals(origin)
                && properties.clientHeaderValue().equals(client);
    }

    private void writeRejection(HttpServletResponse response) throws IOException {
        ApiError body = ApiError.of(ErrorCode.ORIGIN_REJECTED, ErrorCode.ORIGIN_REJECTED.detail(), null, null);
        response.setStatus(ErrorCode.ORIGIN_REJECTED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

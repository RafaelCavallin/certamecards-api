package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

class ApiAccessDeniedHandlerTest {

    private final ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(new ObjectMapper());

    @Test
    void givenAccessDeniedException_whenHandling_thenWritesForbiddenApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = mock(HttpServletRequest.class);

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"forbidden\"");
    }
}

package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

class ApiAccessDeniedHandlerTest {

    private final ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(new ObjectMapper());

    @Test
    void givenAccessDeniedException_whenHandling_thenWritesForbiddenApiError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/decks");

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"forbidden\"");
    }

    @Test
    void givenOfficialAdminWrite_whenHandling_thenStillRespondsForbidden() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/admin/official-cards/1");

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
    }
}

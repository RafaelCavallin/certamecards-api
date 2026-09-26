package br.com.certamecards.common.security;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.PostgresContainerSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class UnauthenticatedApiAccessIT {

    private static final String ANGULAR_HTTP_CLIENT_ACCEPT = "application/json, text/plain, */*";
    private static final String BROWSER_NAVIGATION_ACCEPT = "text/html,application/xhtml+xml,*/*;q=0.8";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {ANGULAR_HTTP_CLIENT_ACCEPT, BROWSER_NAVIGATION_ACCEPT, "*/*"})
    void givenNoAccessToken_whenReadingProtectedApi_thenRespondsUnauthorizedWithoutRedirect(String accept)
            throws Exception {
        mockMvc.perform(get("/api/sync/changes?cursor=0").header(HttpHeaders.ACCEPT, accept))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
    }

    @ParameterizedTest
    @ValueSource(strings = {ANGULAR_HTTP_CLIENT_ACCEPT, BROWSER_NAVIGATION_ACCEPT})
    void givenNoAccessToken_whenWritingProtectedApi_thenRespondsUnauthorizedWithoutRedirect(String accept)
            throws Exception {
        mockMvc.perform(post("/api/sync/mutations")
                        .header(HttpHeaders.ACCEPT, accept)
                        .header("Origin", "https://certamecards.localhost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}

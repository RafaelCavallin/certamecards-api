package br.com.certamecards.testsupport.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.certamecards.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class TestSupportAccountApiIT {

    private static final String PASSWORD = "senha-forte-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenNewTestUser_whenLoggingInRightAway_thenProtectedEndpointsAcceptItWithTermsAccepted() throws Exception {
        String email = "Irene.Candidata@Exemplo.com";

        mockMvc.perform(post("/api/test-support/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody(email, "Irene")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        String token = login("irene.candidata@exemplo.com");
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("irene.candidata@exemplo.com"))
                .andExpect(jsonPath("$.role").value("candidate"))
                .andExpect(jsonPath("$.termsAccepted").value(true));
        mockMvc.perform(get("/api/me/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"));
    }

    @Test
    void givenInvalidEmail_whenCreatingTestUser_thenRejectsWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/test-support/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("sem-arroba", "Otília")))
                .andExpect(status().isBadRequest());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asString();
    }

    private String userBody(String email, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"" + displayName
                + "\",\"timeZone\":\"America/Sao_Paulo\"}";
    }
}

package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.support.PostgresContainerSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class ForwardedHeadersIT {

    private static final String PUBLIC_HOST = "certamecards.localhost";

    @LocalServerPort
    private int port;

    @Test
    void givenRequestFromTrustedProxyWithForwardedHeaders_whenBuildingAbsoluteUrls_thenUsesPublicHttpsOrigin()
            throws Exception {
        HttpResponse<Void> response = get(true);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).contains("https://" + PUBLIC_HOST + "/"));
    }

    @Test
    void givenRequestWithoutForwardedHeaders_whenBuildingAbsoluteUrls_thenKeepsDirectHttpOrigin() throws Exception {
        HttpResponse<Void> response = get(false);

        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).contains("http://localhost:" + port + "/"));
    }

    private HttpResponse<Void> get(boolean forwarded) throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/sync/changes?cursor=0"));
        if (forwarded) {
            request.header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", PUBLIC_HOST);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.GET().build(), HttpResponse.BodyHandlers.discarding());
        }
    }
}

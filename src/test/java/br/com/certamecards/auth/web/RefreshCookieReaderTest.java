package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.auth.service.RefreshCookieProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RefreshCookieReaderTest {

    private final RefreshCookieReader reader =
            new RefreshCookieReader(new RefreshCookieProperties("__Host-certame_rt", Duration.ofDays(30), true));

    @Test
    void givenMatchingCookie_whenReading_thenReturnsItsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("__Host-certame_rt", "raw-token"));

        assertThat(reader.read(request)).isEqualTo("raw-token");
    }

    @Test
    void givenNoCookiesAtAll_whenReading_thenThrowsUnauthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> reader.read(request))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void givenCookiesWithoutMatchingName_whenReading_thenThrowsUnauthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other-cookie", "value"));

        assertThatThrownBy(() -> reader.read(request))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }
}

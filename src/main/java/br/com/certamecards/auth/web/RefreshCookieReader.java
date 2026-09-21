package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.RefreshCookieProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieReader {

    private final RefreshCookieProperties properties;

    public RefreshCookieReader(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw ApiException.of(ErrorCode.UNAUTHENTICATED);
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.name().equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHENTICATED));
    }
}

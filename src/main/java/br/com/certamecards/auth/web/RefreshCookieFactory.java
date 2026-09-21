package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.RefreshCookieProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieFactory {

    private final RefreshCookieProperties properties;

    public RefreshCookieFactory(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String rawToken) {
        return builder(rawToken).maxAge(properties.ttl()).build();
    }

    public ResponseCookie expire() {
        return builder("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(properties.name(), value)
                .path("/")
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite("Strict");
    }
}

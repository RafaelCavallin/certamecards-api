package br.com.certamecards.auth.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "certame_oauth2_req";
    static final String INTENT_COOKIE_NAME = "certame_oauth2_intent";
    private static final int COOKIE_MAX_AGE_SECONDS = 300;

    private final CookieCipher cipher;

    public CookieAuthorizationRequestRepository(CookieCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request, COOKIE_NAME).map(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        addCookie(response, COOKIE_NAME, serialize(authorizationRequest));
        String intent = request.getParameter("intent") == null ? "login" : request.getParameter("intent");
        addCookie(response, INTENT_COOKIE_NAME, intent);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        addCookie(response, COOKIE_NAME, "");
        return authorizationRequest;
    }

    public String readIntent(HttpServletRequest request) {
        return readCookie(request, INTENT_COOKIE_NAME).orElse("login");
    }

    private java.util.Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue);
    }

    private void addCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(value.isEmpty() ? 0 : COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(authorizationRequest);
            }
            return cipher.encrypt(bytes.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String encoded) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(cipher.decrypt(encoded)))) {
            return (OAuth2AuthorizationRequest) in.readObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

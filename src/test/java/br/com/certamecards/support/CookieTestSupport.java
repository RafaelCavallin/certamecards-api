package br.com.certamecards.support;

import jakarta.servlet.http.Cookie;

public final class CookieTestSupport {

    private CookieTestSupport() {}

    public static Cookie fromSetCookieHeader(String setCookieHeader) {
        String firstPair = setCookieHeader.split(";", 2)[0];
        int separator = firstPair.indexOf('=');
        String name = firstPair.substring(0, separator);
        String value = firstPair.substring(separator + 1);
        return new Cookie(name, value);
    }
}

package br.com.certamecards.auth.service;

public final class GoogleTokenPayload {

    private GoogleTokenPayload() {}

    public static String quote(String value) {
        return "\"" + value + "\"";
    }

    public static String unquote(String value) {
        return value.substring(1, value.length() - 1);
    }
}

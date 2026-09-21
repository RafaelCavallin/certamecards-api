package br.com.certamecards.auth.domain;

public enum OauthProvider {
    GOOGLE("google");

    private final String code;

    OauthProvider(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static OauthProvider fromCode(String code) {
        for (OauthProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OauthProvider code: " + code);
    }
}

package br.com.certamecards.auth.domain;

public enum OneTimeTokenPurpose {
    CONFIRM_EMAIL("confirm_email"),
    RESET_PASSWORD("reset_password"),
    LINK_GOOGLE("link_google"),
    REAUTH("reauth");

    private final String code;

    OneTimeTokenPurpose(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static OneTimeTokenPurpose fromCode(String code) {
        for (OneTimeTokenPurpose purpose : values()) {
            if (purpose.code.equals(code)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unknown OneTimeTokenPurpose code: " + code);
    }
}

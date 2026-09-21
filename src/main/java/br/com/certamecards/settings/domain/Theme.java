package br.com.certamecards.settings.domain;

public enum Theme {
    NOITE("noite"),
    DIA("dia"),
    AUTO("auto");

    private final String code;

    Theme(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Theme fromCode(String code) {
        for (Theme theme : values()) {
            if (theme.code.equals(code)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("Unknown Theme code: " + code);
    }
}

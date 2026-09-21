package br.com.certamecards.user.domain;

public enum UserRole {
    CANDIDATE("candidate"),
    ADMIN("admin");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown UserRole code: " + code);
    }
}

package br.com.certamecards.auditlog.domain;

public enum AuditTargetType {
    SUBJECT("subject"),
    ADMIN_ROLE("admin_role"),
    OFFICIAL_DECK("official_deck"),
    OFFICIAL_CARD("official_card");

    private final String code;

    AuditTargetType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

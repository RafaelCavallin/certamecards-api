package br.com.certamecards.auditlog.domain;

public enum AuditAction {
    SUBJECT_CREATED("subject_created"),
    SUBJECT_RENAMED("subject_renamed"),
    SUBJECT_DEACTIVATED("subject_deactivated"),
    SUBJECT_REACTIVATED("subject_reactivated"),
    ADMIN_GRANTED("admin_granted"),
    ADMIN_REVOKED("admin_revoked"),
    OFFICIAL_DECK_CREATED("official_deck_created"),
    OFFICIAL_DECK_UPDATED("official_deck_updated"),
    OFFICIAL_DECK_PUBLISHED("official_deck_published"),
    OFFICIAL_DECK_UNPUBLISHED("official_deck_unpublished"),
    OFFICIAL_DECK_DISCONTINUED("official_deck_discontinued"),
    OFFICIAL_DECK_DELETED("official_deck_deleted"),
    OFFICIAL_CARD_CREATED("official_card_created"),
    OFFICIAL_CARD_UPDATED("official_card_updated"),
    OFFICIAL_CARD_CONTENT_CHANGED("official_card_content_changed"),
    OFFICIAL_CARD_DELETED("official_card_deleted"),
    ERROR_REPORT_RESOLVED("error_report_resolved"),
    ERROR_REPORT_REJECTED("error_report_rejected");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuditAction fromCode(String code) {
        for (AuditAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown AuditAction code: " + code);
    }
}
